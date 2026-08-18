package com.winlator.star.store

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Html
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.winlator.star.store.download.DownloadRegistry
import com.winlator.star.store.download.DownloadScope
import com.winlator.star.store.download.DownloadState
import com.winlator.star.store.download.DownloadsButton
import com.winlator.star.store.download.INSTALLED_GREEN
import com.winlator.star.store.download.InfoChip
import com.winlator.star.store.download.Store
import com.winlator.star.store.download.StoreActionButton
import com.winlator.star.store.download.StoreActionRow
import com.winlator.star.store.download.StoreBadge
import com.winlator.star.store.download.StoreDetailHeader
import com.winlator.star.store.download.StoreDetailState
import com.winlator.star.store.download.StoreHero
import com.winlator.star.store.download.StoreProgressBar
import com.winlator.star.store.download.StoreDownloadHooks
import com.winlator.star.store.download.StoreSection
import com.winlator.star.store.download.StoreStatusText
import com.winlator.star.ui.theme.WinlatorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class EpicGameDetailActivity : ComponentActivity() {

    companion object {
        const val RESULT_REFRESH = 100
        private const val TAG = "BH_EPIC_DETAIL"
        private const val REQUEST_FOLDER_PICKER = 200
    }

    private var prefs: SharedPreferences? = null

    private var appName: String? = null
    private var title: String? = null
    private var description: String? = null
    private var developer: String? = null
    private var artCover: String? = null
    private var namespace: String? = null
    private var catalogItemId: String? = null

    private var exeNameText by mutableStateOf("")
    private var installBtnText by mutableStateOf("Install")
    private var installBtnColor by mutableIntStateOf(0xFF1A73E8.toInt())
    private var launchBtnVisible by mutableStateOf(false)
    private var installBtnVisible by mutableStateOf(true)
    private var setExeBtnVisible by mutableStateOf(false)
    private var uninstallBtnVisible by mutableStateOf(false)
    private var progressVisible by mutableStateOf(false)
    private var progressValue by mutableIntStateOf(0)
    private var progressLabelText by mutableStateOf("")
    private var progressLabelVisible by mutableStateOf(false)
    private var sizeText by mutableStateOf("Fetching\u2026")
    private var cancelDownload: Runnable? = null

    private var updateStatusText by mutableStateOf("")
    private var checkUpdatesEnabled by mutableStateOf(true)
    private var updateBtnVisible by mutableStateOf(false)

    private var dlcJson by mutableStateOf<String?>(null)

    private var cloudSaveDirText by mutableStateOf("No save folder set")
    private var cloudSaveDirColor by mutableIntStateOf(0xFF445566.toInt())
    private var cloudSaveStatusText by mutableStateOf("")
    private var cloudSaveStatusVisible by mutableStateOf(false)
    private var cloudButtonsEnabled by mutableStateOf(true)

    // Themed auto-dismiss bar — system Toasts render as an unreadable black box on this ROM
    // (targetSDK 28); reuse the shared UninstallResultBar for readable feedback.
    private var resultBarMsg by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("bh_epic_prefs", 0)

        // Cross-store Download Manager (Phase C): Epic can download without the Steam foreground
        // service ever running, so init the registry + seed/self-heal the installed library here
        // (idempotent) — mirrors Amazon/GOG.
        DownloadRegistry.init(this)
        EpicLibrarySync.seed(this)

        appName = intent.getStringExtra("app_name")
        title = intent.getStringExtra("title")
        description = intent.getStringExtra("description")
        developer = intent.getStringExtra("developer")
        artCover = intent.getStringExtra("art_cover")
        namespace = intent.getStringExtra("namespace")
        catalogItemId = intent.getStringExtra("catalog_item_id")

        if (appName == null) { finish(); return }

        val savedDir = prefs!!.getString("epic_save_dir_$appName", null)
        if (savedDir != null) {
            cloudSaveDirText = shortenPath(savedDir)
            cloudSaveDirColor = 0xFFCCCCCC.toInt()
        }

        dlcJson = catalogItemId?.let { prefs!!.getString("epic_dlcs_$it", null) }

        setContent {
            WinlatorTheme {
                EpicGameDetailScreen(
                    appName = appName!!,
                    title = title ?: "",
                    description = description ?: "",
                    developer = developer ?: "",
                    artCover = artCover ?: "",
                    namespace = namespace ?: "",
                    catalogItemId = catalogItemId ?: "",
                    exeNameText = exeNameText,
                    installBtnText = installBtnText,
                    launchBtnVisible = launchBtnVisible,
                    installBtnVisible = installBtnVisible,
                    setExeBtnVisible = setExeBtnVisible,
                    uninstallBtnVisible = uninstallBtnVisible,
                    progressVisible = progressVisible,
                    progressValue = progressValue,
                    progressLabelText = progressLabelText,
                    progressLabelVisible = progressLabelVisible,
                    sizeText = sizeText,
                    updateStatusText = updateStatusText,
                    checkUpdatesEnabled = checkUpdatesEnabled,
                    updateBtnVisible = updateBtnVisible,
                    dlcJson = dlcJson,
                    cloudSaveDirText = cloudSaveDirText,
                    cloudSaveStatusText = cloudSaveStatusText,
                    cloudSaveStatusVisible = cloudSaveStatusVisible,
                    cloudButtonsEnabled = cloudButtonsEnabled,
                    onBack = { finish() },
                    onLaunchClick = { pendingLaunchExe() },
                    onInstallClick = { onInstallClicked() },
                    onSetExeClick = { onSetExeClicked() },
                    onUninstallClick = { confirmUninstall() },
                    onCheckUpdates = { doCheckUpdate() },
                    onUpdateClick = {
                        updateBtnVisible = false
                        updateStatusText = "Updating\u2026"
                        startInstallInternal()
                    },
                    onDlcInstall = { dlcApp, dlcNs, dlcCat, dlcTitle ->
                        dlcInstall(dlcApp, dlcNs, dlcCat, dlcTitle)
                    },
                    onCloudBrowse = {
                        startActivityForResult(
                            Intent(this@EpicGameDetailActivity, FolderPickerActivity::class.java),
                            REQUEST_FOLDER_PICKER,
                        )
                    },
                    onCloudUpload = { cloudUpload() },
                    onCloudDownload = { cloudDownload() },
                )
                resultBarMsg?.let { UninstallResultBar(it) { resultBarMsg = null } }
            }
        }

        refreshActionState()
        observeRegistry()
        loadInstallSize()
    }

    override fun onBackPressed() {
        // Leaving the detail page no longer cancels the download: it keeps running on DownloadScope
        // with the foreground-service notification, and can still be cancelled from the Download
        // Manager. Mirrors Amazon/GOG. (Epic's cancel is best-effort anyway — see startInstallInternal.)
        super.onBackPressed()
    }

    /**
     * Make the detail page a live reflection of [DownloadRegistry] for THIS game. Without this,
     * opening the page while a download is live (started from the games list, or after the Activity
     * was recreated) showed "Install" even though the DL-manager card + shade notification were
     * progressing — the page only read install prefs. Runs on the main dispatcher (lifecycleScope
     * default) so the Compose state writes are main-thread-safe. Mirrors GogGameDetailActivity.
     */
    private fun observeRegistry() {
        val an = appName ?: return
        val myKey = "${Store.EPIC}:$an"
        lifecycleScope.launch {
            DownloadRegistry.entries.collect { list ->
                val e = list.firstOrNull { it.key == myKey }
                if (e != null && (e.state == DownloadState.DOWNLOADING || e.state == DownloadState.PAUSED)) {
                    progressVisible = true
                    progressValue = e.pct
                    // Epic is pct-only → live "$pct%" label (matches the DL card + notification, and
                    // the local onProgress below, so a list-started/reopened DL reads identically).
                    progressLabelText = "Downloading… ${e.pct}%"
                    progressLabelVisible = true
                    installBtnVisible = true
                    installBtnText = "Cancel"
                    installBtnColor = 0xFFCC3333.toInt()
                    launchBtnVisible = false
                    setExeBtnVisible = false
                    uninstallBtnVisible = false
                    // Route Cancel to the registry entry so it works for a list-started download —
                    // but ONLY if we don't already hold the local canceller (a locally-started
                    // download sets it in startInstallInternal); guards against any recursion.
                    if (cancelDownload == null) cancelDownload = Runnable { e.cancel?.invoke() }
                } else {
                    // No active entry (absent / INSTALLED / FAILED / CANCELLED): settle from prefs.
                    progressVisible = false
                    progressLabelVisible = false
                    refreshActionState()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FOLDER_PICKER && resultCode == RESULT_OK && data != null) {
            val selectedPath = data.getStringExtra("path")
            if (!selectedPath.isNullOrEmpty()) {
                prefs!!.edit().putString("epic_save_dir_$appName", selectedPath).apply()
                cloudSaveDirText = shortenPath(selectedPath)
                cloudSaveDirColor = 0xFFCCCCCC.toInt()
                cloudButtonsEnabled = true
                resultBarMsg = "Save folder set"
            }
        }
    }

    private fun refreshActionState() {
        val exe = prefs!!.getString("epic_exe_$appName", null)
        val dir = prefs!!.getString("epic_dir_$appName", null)
        val installed = exe != null

        exeNameText = if (installed) ".exe: ${File(exe!!).name}" else ""
        launchBtnVisible = installed
        installBtnVisible = !installed
        setExeBtnVisible = installed
        uninstallBtnVisible = dir != null

        if (!installed) installBtnText = "Install"
    }

    private fun onInstallClicked() {
        if (installBtnText == "Cancel") {
            cancelDownload?.run(); cancelDownload = null
            return
        }
        startInstallInternal()
    }

    private fun startInstallInternal() {
        val an = appName ?: return
        installBtnText = "Cancel"
            installBtnColor = 0xFFCC3333.toInt()
        progressVisible = true
        progressLabelVisible = true
        launchBtnVisible = false
        setExeBtnVisible = false
        progressLabelText = ""

        // WEAK CANCEL (Epic-only): install() takes no cancel checker, so flipping this flag can't
        // abort mid-download \u2014 it's only read AFTER install() returns, at which point the finished
        // download is discarded. The registry row/notification clear then. Honest, best-effort.
        val cancelled = AtomicBoolean(false)
        cancelDownload = Runnable { cancelled.set(true) }

        // Publish into the cross-store Download Manager (shade notification + process kept alive).
        // Epic reports pct only \u2192 single honest bar (byte pairs left at 0).
        StoreDownloadHooks.registerDownload(
            store = Store.EPIC,
            id = an,
            name = title ?: an,
            cover = artCover,
            supportsPause = false,
            installTotal = prefs!!.getLong("epic_size_$an", 0L),
            cancel = { cancelled.set(true) },
        )

        // applicationContext + DownloadScope.io (NOT lifecycleScope): install() is a synchronous
        // blocking call, so the download now survives this Activity being destroyed / backgrounded.
        // Registry hooks are Activity-independent; only the mutableState UI writes are lifecycle-guarded.
        val appCtx = applicationContext
        DownloadScope.io.launch {
            try {
                val token = EpicCredentialStore.getValidAccessToken(appCtx)
                if (token == null) { onInstallError("Login required"); return@launch }

                if (!isDestroyed && !isFinishing) runOnUiThread { progressLabelText = "Fetching manifest\u2026" }
                val manifestJson = EpicApiClient.getManifestApiJson(token, namespace, catalogItemId, an)
                if (manifestJson == null) { onInstallError("Failed to fetch manifest"); return@launch }

                var sanitized = (title ?: "").replace(Regex("[^a-zA-Z0-9 \\-_]"), "").trim()
                if (sanitized.isEmpty()) sanitized = "epic_${an.hashCode()}"
                val installDir = File(File(filesDir, "epic_games"), sanitized)
                prefs!!.edit().putString("epic_dir_$an", installDir.absolutePath).apply()

                val ok = EpicDownloadManager.install(
                    appCtx,
                    manifestJson,
                    token,
                    installDir.absolutePath,
                ) { _, pct ->
                    // Freeze the card/label the moment the user hits Cancel (the download itself
                    // can't be stopped \u2014 see WEAK CANCEL above).
                    if (!cancelled.get()) {
                        StoreDownloadHooks.tick(Store.EPIC, an, pct)
                        if (!isDestroyed && !isFinishing) runOnUiThread {
                            if (isDestroyed || isFinishing) return@runOnUiThread
                            progressValue = pct
                            progressLabelText = "Downloading\u2026 $pct%"
                        }
                    }
                }

                if (cancelled.get()) { onInstallCancelled(); return@launch }
                if (!ok) { onInstallError("Download failed"); return@launch }

                try {
                    val vid = JSONObject(manifestJson).optString("versionId", "")
                    if (vid.isNotEmpty()) {
                        prefs!!.edit().putString("epic_manifest_version_$an", vid).apply()
                    }
                } catch (_: Exception) {}

                val exeFiles = mutableListOf<File>()
                AmazonLaunchHelper.collectExe(installDir, exeFiles)
                if (exeFiles.isEmpty()) { onInstallError("No executable found"); return@launch }

                val lowerTitle = (title ?: "").lowercase()
                exeFiles.sortWith { a, b ->
                    AmazonLaunchHelper.scoreExe(b, lowerTitle) - AmazonLaunchHelper.scoreExe(a, lowerTitle)
                }

                // Completion NEVER shows a dialog: auto-record the best-scored exe (list already
                // sorted best-first) and finalize \u2014 mirrors the Amazon/GOG fix that unwedged the
                // 100%-stuck card when the user wasn't on the detail page. Exe choice stays
                // available via "Set .exe\u2026".
                prefs!!.edit().putString("epic_exe_$an", exeFiles[0].absolutePath).apply()
                onInstallComplete()
            } catch (e: Exception) {
                if (!cancelled.get()) onInstallError(e.message ?: "Unknown error")
            }
        }
    }

    private fun onInstallComplete() {
        cancelDownload = null
        // Finalize into the registry regardless of which screen is showing (fixes the 100% wedge).
        appName?.let { an ->
            prefs!!.getString("epic_dir_$an", null)?.let { dir ->
                StoreDownloadHooks.markInstalled(
                    store = Store.EPIC,
                    id = an,
                    installPath = dir,
                    bytes = prefs!!.getLong("epic_size_$an", 0L),
                )
            }
        }
        if (!isDestroyed && !isFinishing) runOnUiThread {
            if (isDestroyed || isFinishing) return@runOnUiThread
            progressVisible = false
            progressLabelVisible = false
            setResult(RESULT_REFRESH)
            refreshActionState()
        }
    }

    private fun onInstallError(msg: String) {
        cancelDownload = null
        appName?.let { StoreDownloadHooks.markFailed(Store.EPIC, it, msg) }
        if (!isDestroyed && !isFinishing) runOnUiThread {
            if (isDestroyed || isFinishing) return@runOnUiThread
            progressVisible = false
            progressLabelVisible = false
            installBtnText = "Install"
            installBtnColor = 0xFF1A73E8.toInt()
            launchBtnVisible = true
            setExeBtnVisible = true
            resultBarMsg = "Error: $msg"
        }
    }

    private fun onInstallCancelled() {
        cancelDownload = null
        appName?.let { StoreDownloadHooks.markCancelled(Store.EPIC, it) }
        if (!isDestroyed && !isFinishing) runOnUiThread {
            if (isDestroyed || isFinishing) return@runOnUiThread
            progressVisible = false
            progressLabelVisible = false
            installBtnText = "Install"
            installBtnColor = 0xFF1A73E8.toInt()
            launchBtnVisible = true
            setExeBtnVisible = true
        }
    }

    private fun confirmUninstall() {
        AlertDialog.Builder(this)
            .setTitle("Uninstall $title?")
            .setMessage("This will delete all installed game files.")
            .setPositiveButton("Uninstall") { _, _ ->
                val an = appName ?: return@setPositiveButton
                val dir = prefs!!.getString("epic_dir_$an", null) ?: return@setPositiveButton
                lifecycleScope.launch(Dispatchers.IO) {
                    deleteDir(File(dir))
                    // Purge the FULL native install record via the canonical helper + clear the DL
                    // manager's registry/library row, so the store list and Download Manager stay in
                    // sync. Mirrors Amazon/GOG.
                    EpicInstallState.purge(applicationContext, an)
                    StoreDownloadHooks.markUninstalled(Store.EPIC, an)
                    runOnUiThread {
                        setResult(RESULT_REFRESH)
                        refreshActionState()
                        resultBarMsg = "$title uninstalled"
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pendingLaunchExe() {
        val exe = prefs!!.getString("epic_exe_$appName", null) ?: return
        // Mirror the Epic games-list Launch (StarLaunchBridge container picker). The old hardcoded
        // LandscapeLauncherMainActivity component doesn't exist in this app (com.winlator.banner)
        // and crashed with ActivityNotFoundException — identical to the Amazon detail bug.
        StarLaunchBridge.addToLauncher(this, title ?: appName ?: "Game", exe, artCover ?: "")
    }

    private fun onSetExeClicked() {
        val dir = prefs!!.getString("epic_dir_$appName", null) ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            val exeFiles = mutableListOf<File>()
            AmazonLaunchHelper.collectExe(File(dir), exeFiles)
            if (exeFiles.isEmpty()) {
                runOnUiThread { resultBarMsg = "No .exe files found" }
                return@launch
            }
            val candidates = exeFiles.map { it.absolutePath }
            runOnUiThread {
                showExePicker(candidates) { selected ->
                    if (!selected.isNullOrEmpty()) {
                        prefs!!.edit().putString("epic_exe_$appName", selected).apply()
                        refreshActionState()
                        setResult(RESULT_REFRESH)
                        resultBarMsg = "Exe set: ${File(selected).name}"
                    }
                }
            }
        }
    }

    private fun loadInstallSize() {
        val cached = prefs!!.getLong("epic_size_$appName", -1L)
        if (cached > 0) {
            sizeText = formatBytes(cached)
            return
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val token = EpicCredentialStore.getValidAccessToken(this@EpicGameDetailActivity)
            val size = if (token != null)
                EpicDownloadManager.fetchInstallSizeBytes(token, namespace, catalogItemId, appName)
            else -1L
            if (size > 0) prefs!!.edit().putLong("epic_size_$appName", size).apply()
            val finalSize = size
            runOnUiThread {
                sizeText = if (finalSize > 0) formatBytes(finalSize) else "Unknown"
            }
        }
    }

    private fun doCheckUpdate() {
        updateStatusText = "Checking\u2026"
        checkUpdatesEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = EpicCredentialStore.getValidAccessToken(this@EpicGameDetailActivity)
                if (token == null) {
                    runOnUiThread {
                        checkUpdatesEnabled = true
                        updateStatusText = "Login required."
                    }
                    return@launch
                }
                val manifestJson = EpicApiClient.getManifestApiJson(token, namespace, catalogItemId, appName)
                var latestVer: String? = null
                if (manifestJson != null) {
                    try { latestVer = JSONObject(manifestJson).optString("versionId", null) } catch (_: Exception) {}
                }
                val latest = latestVer
                runOnUiThread {
                    checkUpdatesEnabled = true
                    if (latest.isNullOrEmpty()) {
                        updateStatusText = "Could not reach update server."
                        return@runOnUiThread
                    }
                    val stored = prefs!!.getString("epic_manifest_version_$appName", null)
                    if (stored == null) {
                        prefs!!.edit().putString("epic_manifest_version_$appName", latest).apply()
                        updateStatusText = "Up to date \u2713"
                        updateBtnVisible = false
                    } else if (stored == latest) {
                        updateStatusText = "Up to date \u2713"
                        updateBtnVisible = false
                    } else {
                        updateStatusText = "Update available!\nInstalled: ${stored.substring(0, minOf(12, stored.length))}\u2026  \u2192  Latest: ${latest.substring(0, minOf(12, latest.length))}\u2026"
                        updateBtnVisible = true
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    checkUpdatesEnabled = true
                    updateStatusText = "Check failed: ${e.message}"
                }
            }
        }
    }

    private fun dlcInstall(dlcApp: String, dlcNs: String, dlcCat: String, dlcTitle: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val token = EpicCredentialStore.getValidAccessToken(this@EpicGameDetailActivity)
                if (token == null) {
                    runOnUiThread { resultBarMsg = "Login required" }
                    return@launch
                }

                val manifestJson = EpicApiClient.getManifestApiJson(token, dlcNs, dlcCat, dlcApp)
                if (manifestJson == null) {
                    runOnUiThread { resultBarMsg = "Failed to fetch manifest for DLC" }
                    return@launch
                }

                var sanitized = dlcTitle.replace(Regex("[^a-zA-Z0-9 \\-_]"), "").trim()
                if (sanitized.isEmpty()) sanitized = "dlc_${dlcApp.hashCode()}"
                val installDir = File(File(filesDir, "epic_games"), sanitized)
                prefs!!.edit().putString("epic_dir_$dlcApp", installDir.absolutePath).apply()

                val ok = EpicDownloadManager.install(
                    this@EpicGameDetailActivity,
                    manifestJson,
                    token,
                    installDir.absolutePath,
                ) { _, _ -> }
                if (!ok) {
                    runOnUiThread { resultBarMsg = "DLC download failed" }
                    return@launch
                }

                val exeFiles = mutableListOf<File>()
                AmazonLaunchHelper.collectExe(installDir, exeFiles)
                if (exeFiles.isNotEmpty()) {
                    val lowerT = dlcTitle.lowercase()
                    exeFiles.sortWith { a, b ->
                        AmazonLaunchHelper.scoreExe(b, lowerT) - AmazonLaunchHelper.scoreExe(a, lowerT)
                    }
                    prefs!!.edit().putString("epic_exe_$dlcApp", exeFiles[0].absolutePath).apply()
                }

                runOnUiThread {
                    setResult(RESULT_REFRESH)
                    refreshActionState()
                    resultBarMsg = "$dlcTitle installed"
                }
            } catch (e: Exception) {
                runOnUiThread { resultBarMsg = "DLC install error: ${e.message}" }
            }
        }
    }

    private fun cloudUpload() {
        val dir = prefs!!.getString("epic_save_dir_$appName", null)
        if (dir == null) { resultBarMsg = "Set a save folder first"; return }
        cloudButtonsEnabled = false
        cloudSaveStatusText = "Preparing upload\u2026"
        cloudSaveStatusVisible = true
        EpicCloudSaveManager.uploadSaves(this, appName!!, File(dir), object : EpicCloudSaveManager.Callback {
            override fun onStatus(msg: String) { runOnUiThread { cloudSaveStatusText = msg } }
            override fun onDone(msg: String) { runOnUiThread { cloudSaveStatusText = msg; cloudButtonsEnabled = true } }
            override fun onError(msg: String) { runOnUiThread { cloudSaveStatusText = "Error: $msg"; cloudButtonsEnabled = true } }
        })
    }

    private fun cloudDownload() {
        val dir = prefs!!.getString("epic_save_dir_$appName", null)
        if (dir == null) { resultBarMsg = "Set a save folder first"; return }
        cloudButtonsEnabled = false
        cloudSaveStatusText = "Preparing download\u2026"
        cloudSaveStatusVisible = true
        EpicCloudSaveManager.downloadSaves(this, appName!!, File(dir), object : EpicCloudSaveManager.Callback {
            override fun onStatus(msg: String) { runOnUiThread { cloudSaveStatusText = msg } }
            override fun onDone(msg: String) { runOnUiThread { cloudSaveStatusText = msg; cloudButtonsEnabled = true } }
            override fun onError(msg: String) { runOnUiThread { cloudSaveStatusText = "Error: $msg"; cloudButtonsEnabled = true } }
        })
    }

    private fun showExePicker(candidates: List<String>, onSelected: (String?) -> Unit) {
        val labels = candidates.map { path ->
            val f = File(path)
            val parent = f.parentFile
            (if (parent != null) "${parent.name}/${f.name}" else f.name)
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select game executable")
            .setItems(labels) { _, which -> onSelected(candidates[which]) }
            .setCancelable(false)
            .show()
    }

    private fun deleteDir(dir: File) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { f -> if (f.isDirectory) deleteDir(f) else f.delete() }
        dir.delete()
    }

    private fun shortenPath(path: String): String {
        val parts = path.split("/")
        if (parts.size <= 3) return path
        return "\u2026/${parts[parts.size - 2]}/${parts[parts.size - 1]}"
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1_073_741_824L -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576L -> "%.0f MB".format(bytes / 1_048_576.0)
        else -> "$bytes B"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpicGameDetailScreen(
    appName: String,
    title: String,
    description: String,
    developer: String,
    artCover: String,
    namespace: String,
    catalogItemId: String,
    exeNameText: String,
    installBtnText: String,
    launchBtnVisible: Boolean,
    installBtnVisible: Boolean,
    setExeBtnVisible: Boolean,
    uninstallBtnVisible: Boolean,
    progressVisible: Boolean,
    progressValue: Int,
    progressLabelText: String,
    progressLabelVisible: Boolean,
    sizeText: String,
    updateStatusText: String,
    checkUpdatesEnabled: Boolean,
    updateBtnVisible: Boolean,
    dlcJson: String?,
    cloudSaveDirText: String,
    cloudSaveStatusText: String,
    cloudSaveStatusVisible: Boolean,
    cloudButtonsEnabled: Boolean,
    onBack: () -> Unit,
    onLaunchClick: () -> Unit,
    onInstallClick: () -> Unit,
    onSetExeClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onCheckUpdates: () -> Unit,
    onUpdateClick: () -> Unit,
    onDlcInstall: (String, String, String, String) -> Unit,
    onCloudBrowse: () -> Unit,
    onCloudUpload: () -> Unit,
    onCloudDownload: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("bh_epic_prefs", 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Header — back + Epic badge + Download Manager button (Steam parity).
        StoreDetailHeader(
            onBack = onBack,
            storeBadge = { StoreBadge(Store.EPIC) },
            actions = { DownloadsButton() },
        )

        // Hero image with the fade into the page background.
        StoreHero {
            if (artCover.isNotEmpty()) {
                AsyncImage(
                    model = artCover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
        }

        // Info section — name + metadata chips + description + install status.
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(sizeText)
                if (developer.isNotEmpty()) InfoChip(developer)
                if (appName.isNotEmpty()) InfoChip("App: $appName")
                val releaseDate = prefs.getString("epic_release_$appName", null)
                if (!releaseDate.isNullOrEmpty()) InfoChip(formatDateStatic(releaseDate))
            }
            if (description.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                val plain = Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT).toString().trim()
                val desc = if (plain.length > 400) "${plain.substring(0, 400)}…" else plain
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (exeNameText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                StoreStatusText(exeNameText, StoreDetailState.INSTALLED)
            }
        }

        // Progress — one honest install bar with its label (Epic reports pct only).
        if (progressVisible) {
            StoreProgressBar(
                pct = progressValue,
                label = if (progressLabelVisible) progressLabelText else null,
            )
        }

        // Actions — weighted M3 buttons; Cancel/Uninstall are destructive (error).
        StoreActionRow {
            if (launchBtnVisible) {
                StoreActionButton(
                    text = "Launch",
                    onClick = onLaunchClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (installBtnVisible) {
                StoreActionButton(
                    text = installBtnText,
                    onClick = onInstallClick,
                    modifier = Modifier.weight(1f),
                    destructive = installBtnText == "Cancel",
                )
            }
            if (setExeBtnVisible) {
                StoreActionButton(
                    text = "Set .exe…",
                    onClick = onSetExeClick,
                    modifier = Modifier.weight(1f),
                )
            }
            if (uninstallBtnVisible) {
                StoreActionButton(
                    text = "Uninstall",
                    onClick = onUninstallClick,
                    modifier = Modifier.weight(1f),
                    destructive = true,
                )
            }
        }

        // Updates
        StoreSection(title = "Updates") {
            val installed = prefs.getString("epic_exe_$appName", null) != null
            if (!installed) {
                StoreStatusText("Install the game first to check for updates.")
            } else {
                val displayText = if (updateStatusText.isNotEmpty()) updateStatusText
                else {
                    val storedVer = prefs.getString("epic_manifest_version_$appName", null)
                    if (storedVer != null) "Installed: ${storedVer.substring(0, minOf(14, storedVer.length))}…"
                    else "Version not recorded — tap Check to verify"
                }
                StoreStatusText(displayText)
                Spacer(Modifier.height(8.dp))
                if (updateBtnVisible) {
                    StoreActionButton(
                        text = "Update Now",
                        onClick = onUpdateClick,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                StoreActionButton(
                    text = "Check for Updates",
                    onClick = onCheckUpdates,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = checkUpdatesEnabled,
                )
            }
        }

        // DLC
        StoreSection(title = "DLC") {
            if (dlcJson.isNullOrEmpty() || dlcJson == "[]") {
                StoreStatusText("No DLCs in your library for this game")
            } else {
                val arr = runCatching { org.json.JSONArray(dlcJson) }.getOrNull()
                if (arr == null) {
                    StoreStatusText("Error reading DLC data")
                } else if (arr.length() == 0) {
                    StoreStatusText("No DLCs in your library for this game")
                } else {
                    Text(
                        text = "${arr.length()} DLC${if (arr.length() == 1) "" else "s"} owned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    for (i in 0 until arr.length()) {
                        val dlc = arr.optJSONObject(i) ?: continue
                        val dlcApp = dlc.optString("app", "")
                        val dlcNs = dlc.optString("ns", "")
                        val dlcCat = dlc.optString("cat", "")
                        val dlcTitle = dlc.optString("title", "Unknown DLC")
                        val dlcInstalled = prefs.getString("epic_exe_$dlcApp", null) != null

                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dlcTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                if (dlcInstalled) {
                                    Text(
                                        text = "✓ Installed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = INSTALLED_GREEN,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                            if (dlcApp.isNotEmpty() && dlcNs.isNotEmpty() && dlcCat.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                StoreActionButton(
                                    text = if (dlcInstalled) "Reinstall" else "Install",
                                    onClick = { onDlcInstall(dlcApp, dlcNs, dlcCat, dlcTitle) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cloud Saves
        StoreSection(title = "Cloud Saves") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = cloudSaveDirText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                StoreActionButton(text = "Browse", onClick = onCloudBrowse)
            }
            if (cloudSaveStatusVisible) {
                Spacer(Modifier.height(8.dp))
                StoreStatusText(cloudSaveStatusText)
            }
            Spacer(Modifier.height(8.dp))
            StoreActionButton(
                text = "Upload Saves",
                onClick = onCloudUpload,
                modifier = Modifier.fillMaxWidth(),
                enabled = cloudButtonsEnabled,
            )
            Spacer(Modifier.height(8.dp))
            StoreActionButton(
                text = "Download Saves",
                onClick = onCloudDownload,
                modifier = Modifier.fillMaxWidth(),
                enabled = cloudButtonsEnabled,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun formatDateStatic(iso: String): String {
    if (iso.length < 10) return iso
    val parts = iso.substring(0, 10).split("-")
    if (parts.size != 3) return iso.substring(0, 10)
    return try {
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        if (month < 1 || month > 12) return iso.substring(0, 10)
        "${months[month - 1]} $day, $year"
    } catch (_: Exception) {
        iso.substring(0, 10)
    }
}
