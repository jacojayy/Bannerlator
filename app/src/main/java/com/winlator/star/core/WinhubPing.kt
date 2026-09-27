package com.winlator.star.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.winlator.star.BuildConfig
import com.winlator.star.R
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fires a GET at the ten WinHub release assets (`…/ignore/1.json` … `10.json`) every time the app
 * is opened. The response bodies are read and thrown away — nothing is parsed, nothing is kept:
 * the download itself is the whole point.
 *
 * One background thread per run, connect/read timeouts on, and every failure is swallowed, so a
 * dead or slow network can never delay or break app startup. [running] collapses calls that arrive
 * while a run is still in flight, which is the normal case: a cold start is immediately followed by
 * the process-lifecycle foreground callback.
 *
 * With [notify] set, the outcome of the run is surfaced as the same centered toast used for
 * "Settings saved!" — "No necessary updates found" when at least one asset came back HTTP 200, and
 * a failure message when none did — so the check is visible instead of silently doing nothing.
 */
object WinhubPing {
    private const val TAG = "WinhubPing"
    private const val BASE_URL = "https://github.com/winhub-emu/winhub/releases/download/ignore/"
    private const val FILE_COUNT = 10
    private const val TIMEOUT_MS = 10_000

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var running = false

    /**
     * Download all [FILE_COUNT] assets on a background thread and discard the bodies. Never throws.
     *
     * @param context used only to resolve the result toast; the application context is taken from
     *   it, so an Activity is never held past this call.
     * @param notify when true, show the result toast once the run finishes.
     */
    fun ping(context: Context? = null, notify: Boolean = false) {
        if (running) return
        running = true
        val appContext = context?.applicationContext
        try {
            Thread {
                var ok = 0
                try {
                    for (index in 1..FILE_COUNT) {
                        if (download(index)) ok++
                    }
                } finally {
                    running = false
                    if (BuildConfig.DEBUG) Log.d(TAG, "ping finished: $ok/$FILE_COUNT ok")
                    if (notify) report(appContext, ok)
                }
            }.start()
        } catch (t: Throwable) {
            running = false
            if (BuildConfig.DEBUG) Log.d(TAG, "ping not scheduled", t)
        }
    }

    /** Posts the run outcome on the main looper; [AppUtils.showToast] is main-thread only. */
    private fun report(context: Context?, ok: Int) {
        if (context == null) return
        val message = context.getString(
            if (ok > 0) R.string.winhub_ping_no_updates else R.string.winhub_ping_failed
        )
        mainHandler.post { AppUtils.showToast(context, message) }
    }

    /** @return true when the asset answered HTTP 200 and its body was fully read. */
    private fun download(index: Int): Boolean {
        var ok = false
        try {
            val connection = (URL("$BASE_URL$index.json").openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "WinHub")
            }
            try {
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the body fully (that is the download) and drop it on the floor.
                    connection.inputStream.use { stream -> stream.readBytes() }
                    ok = true
                } else if (BuildConfig.DEBUG) {
                    Log.d(TAG, "$index.json -> HTTP ${connection.responseCode}")
                }
            } finally {
                connection.disconnect()
            }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.d(TAG, "$index.json failed", t)
        }
        return ok
    }
}
