package com.winlator.star.store

import android.content.Context
import android.os.Environment
import android.util.Log
import com.winlator.star.BuildConfig
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * GitHub OAuth **device flow** for the in-game Social Hub. Deliberately not the web-view/redirect
 * flow: the drawer is a ComposeView inside the fullscreen game activity, there is no redirect URI
 * to come back to, and the user is on a phone where typing a code on a second surface is the
 * normal pattern anyway.
 *
 * Flow:
 *  1. [requestDeviceCode] POST `https://github.com/login/device/code` with the OAuth app's
 *     client_id → a `user_code` (e.g. `ABCD-1234`) + a `device_code` + `verification_uri`.
 *  2. The UI shows the code and opens `https://github.com/login/device` for the user to type it.
 *  3. [pollForToken] POST `https://github.com/login/oauth/access_token` with
 *     `grant_type=urn:ietf:params:oauth:grant-type:device_code` every `interval` seconds until
 *     GitHub answers with `access_token` (or `access_denied` / `expired_token` / `authorization_pending`).
 *  4. [fetchUser] `GET https://api.github.com/user` with `Authorization: Bearer <token>` → login/avatar.
 *
 * Every call here is **blocking**; the caller runs it on Dispatchers.IO (the store does).
 *
 * The token is a classic OAuth access token scoped to `public_repo` — enough to open, edit and
 * comment on issues in the public `winhub-emu/social-hub` repo, and nothing else. It is persisted
 * in plain text at `Downloads/WinHub-Credentials/github.json` (the user's explicit Q2 choice).
 *
 * The client secret is intentionally NOT in this file: the device flow does not need it, and a
 * secret compiled into an APK is not a secret.
 */
object GitHubAuthClient {

    private const val TAG = "GitHubAuthClient"

    /** OAuth app client id (public by design — it ships in the binary). */
    const val CLIENT_ID = "Ov23liZVHgGdS95Kr19U"

    /** Requested scopes. `public_repo` = read/write on public repos (issues + comments); `read:user` = profile. */
    private const val SCOPE = "public_repo read:user"

    private const val DEVICE_CODE_URL = "https://github.com/login/device/code"
    private const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    private const val USER_URL = "https://api.github.com/user"
    private const val LOGIN_DEVICE_URI = "https://github.com/login/device"

    private const val USER_AGENT = "WinHub-SocialHub"

    /** Where the plaintext credential lands (user-chosen location; see file comment). */
    private const val CREDENTIAL_DIR = "WinHub-Credentials"
    private const val CREDENTIAL_FILE = "github.json"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    data class DeviceCode(
        val deviceCode: String,
        val userCode: String,
        /** Where the user types [userCode]; `https://github.com/login/device` always works. */
        val verificationUri: String,
        /** Seconds between polls; GitHub may bump this via `slow_down`. */
        val intervalSeconds: Int,
        val expiresInSeconds: Int,
    )

    data class Account(
        val login: String,
        val userId: Long,
        val avatarUrl: String,
        val accessToken: String,
        val savedAtMs: Long,
    )

    sealed class Poll {
        data class Done(val accessToken: String) : Poll()
        object Pending : Poll()
        /** GitHub asked us to slow down — add 5s to the interval before the next poll. */
        object SlowDown : Poll()
        object Denied : Poll()
        object Expired : Poll()
        data class Failed(val reason: String) : Poll()
    }

    /** True when the OAuth app does not have Device Flow enabled (the current app state). */
    @Volatile
    var deviceFlowDisabled: Boolean = false
        private set

    // ── Step 1: device code ──────────────────────────────────────────────────────────────────────

    /**
     * Blocking. Returns null on any failure; [lastError] carries a user-facing sentence.
     * Sets [deviceFlowDisabled] when GitHub answers `device_flow_disabled` so the UI can say
     * exactly which checkbox is missing rather than a generic "login failed".
     */
    fun requestDeviceCode(): DeviceCode? {
        lastError = null
        val body = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("scope", SCOPE)
            .build()
        val request = Request.Builder()
            .url(DEVICE_CODE_URL)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    lastError = "GitHub refused the sign-in request (HTTP ${resp.code})."
                    if (BuildConfig.DEBUG) Log.w(TAG, "device code HTTP ${resp.code}: $text")
                    return null
                }
                val json = JSONObject(text)
                val error = json.optString("error")
                if (error.isNotEmpty()) {
                    if (error == "device_flow_disabled") {
                        deviceFlowDisabled = true
                        lastError =
                            "Device Flow is not enabled for this app yet. Flip \"Enable Device Flow\" " +
                            "on the OAuth app at github.com/settings/developers, then retry."
                    } else {
                        lastError = json.optString("error_description")
                            .ifBlank { "GitHub returned $error." }
                    }
                    if (BuildConfig.DEBUG) Log.w(TAG, "device code error: $text")
                    return null
                }
                val deviceCode = json.optString("device_code")
                val userCode = json.optString("user_code")
                if (deviceCode.isBlank() || userCode.isBlank()) {
                    lastError = "GitHub returned an incomplete device code."
                    return null
                }
                DeviceCode(
                    deviceCode = deviceCode,
                    userCode = userCode,
                    verificationUri = json.optString("verification_uri").ifBlank { LOGIN_DEVICE_URI },
                    intervalSeconds = json.optInt("interval", 5).coerceAtLeast(1),
                    expiresInSeconds = json.optInt("expires_in", 900),
                )
            }
        } catch (t: Throwable) {
            lastError = "Could not reach github.com. Check your connection."
            if (BuildConfig.DEBUG) Log.w(TAG, "requestDeviceCode", t)
            null
        }
    }

    // ── Step 2: poll for the token ───────────────────────────────────────────────────────────────

    /**
     * Blocking single poll. Call once per [DeviceCode.intervalSeconds]; never loops by itself so
     * the caller owns cancellation (the store's scope).
     */
    fun pollForToken(deviceCode: String): Poll {
        val body = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("device_code", deviceCode)
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .build()
        val request = Request.Builder()
            .url(TOKEN_URL)
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return Poll.Failed("Token poll failed (HTTP ${resp.code}).")
                }
                val json = JSONObject(text)
                val token = json.optString("access_token")
                if (token.isNotBlank()) return Poll.Done(token)
                when (json.optString("error")) {
                    "authorization_pending" -> Poll.Pending
                    "slow_down" -> Poll.SlowDown
                    "access_denied" -> Poll.Denied
                    "expired_token" -> Poll.Expired
                    else -> Poll.Failed(
                        json.optString("error_description").ifBlank { "Sign-in failed." }
                    )
                }
            }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "pollForToken", t)
            Poll.Failed("Could not reach github.com. Check your connection.")
        }
    }

    // ── Step 3: identity ─────────────────────────────────────────────────────────────────────────

    /**
     * Blocking `GET /user`. Returns null if the token is rejected (expired, revoked, wrong scope) —
     * the store treats that as "sign out and start over".
     */
    fun fetchUser(accessToken: String): Account? {
        if (accessToken.isBlank()) return null
        val request = Request.Builder()
            .url(USER_URL)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (BuildConfig.DEBUG) Log.w(TAG, "fetchUser HTTP ${resp.code}")
                    return null
                }
                val json = JSONObject(resp.body?.string().orEmpty())
                val login = json.optString("login")
                if (login.isBlank()) return null
                Account(
                    login = login,
                    userId = json.optLong("id"),
                    avatarUrl = json.optString("avatar_url"),
                    accessToken = accessToken,
                    savedAtMs = System.currentTimeMillis(),
                )
            }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "fetchUser", t)
            null
        }
    }

    // ── Credential persistence (plaintext, by explicit user choice) ─────────────────────────────

    /**
     * Where the token lives: `Downloads/WinHub-Credentials/github.json`. Implemented as a
     * DIRECTORY `WinHub-Credentials` holding `github.json` so future providers (Steam, Epic…)
     * drop in beside it without a rename — stated as an assumption, matching AccountManager's
     * `Downloads/bannerlator/game-configs/BACKUP` shape.
     */
    fun credentialFile(context: Context): File? = try {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, CREDENTIAL_DIR)
        if (!dir.exists()) dir.mkdirs()
        File(dir, CREDENTIAL_FILE)
    } catch (t: Throwable) {
        if (BuildConfig.DEBUG) Log.w(TAG, "credentialFile", t)
        null
    }

    /** Blocking write; world-readable so a manual backup off-device just works. */
    fun save(context: Context, account: Account) {
        val file = credentialFile(context) ?: return
        try {
            JSONObject()
                .put("provider", "github")
                .put("login", account.login)
                .put("user_id", account.userId)
                .put("avatar_url", account.avatarUrl)
                .put("access_token", account.accessToken)
                .put("saved_at", account.savedAtMs)
                .toString(2)
                .also { file.writeText(it) }
            file.setReadable(true, false)
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "save", t)
        }
    }

    /** Blocking read; null when missing, corrupt, or not a GitHub entry. */
    fun load(context: Context): Account? {
        val file = credentialFile(context) ?: return null
        if (!file.exists()) return null
        return try {
            val json = JSONObject(file.readText())
            val token = json.optString("access_token")
            val login = json.optString("login")
            if (token.isBlank() || login.isBlank()) null
            else Account(
                login = login,
                userId = json.optLong("user_id"),
                avatarUrl = json.optString("avatar_url"),
                accessToken = token,
                savedAtMs = json.optLong("saved_at"),
            )
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "load", t)
            null
        }
    }

    /** Blocking delete of the credential file (sign-out). */
    fun clear(context: Context) {
        try {
            credentialFile(context)?.let { if (it.exists()) it.delete() }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.w(TAG, "clear", t)
        }
    }

    /** Last user-facing failure message from [requestDeviceCode]. */
    @Volatile
    var lastError: String? = null
        private set
}
