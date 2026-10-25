package com.winhub.hernandez.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winhub.hernandez.MainActivity
import com.winhub.hernandez.ui.screens.contents.ContentsInstaller
import com.winhub.hernandez.xenvironment.ImageFs
import com.winhub.hernandez.xenvironment.ImageFsInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel(app: Application) : AndroidViewModel(app) {

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling

    /** Overall setup progress: ImageFS occupies the first fifth, then one fifth per component. */
    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    /** 0 = ImageFS, 1..4 = the component at that index, 5 = finished. */
    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step

    private val _status = MutableStateFlow("Preparing system files")
    val status: StateFlow<String> = _status

    /** True when at least one component failed; setup still completes and Proceed is offered. */
    private val _degraded = MutableStateFlow(false)
    val degraded: StateFlow<Boolean> = _degraded

    /** True once installation reaches 100% — show the Proceed button. */
    private val _showProceed = MutableStateFlow(false)
    val showProceed: StateFlow<Boolean> = _showProceed

    /**
     * Check whether the system image needs (re)installation.
     * Returns true if an install was triggered; the caller should show the install overlay.
     */
    fun installIfNeeded(activity: MainActivity): Boolean {
        if (_isInstalling.value) return true   // already running

        val imageFs = ImageFs.find(activity)
        if (imageFs.isValid && imageFs.version >= ImageFsInstaller.LATEST_VERSION) {
            // imagefs is current, so no re-extract — but the frame-gen layers and ffmpeg-8 ship as
            // APK assets alongside it, and an app update can carry newer builds of them. Without
            // this they would only ever land on a full re-extract, so a bundled layer update
            // silently never reached the device. Off the main thread: this can copy several MB.
            viewModelScope.launch(Dispatchers.IO) {
                ImageFsInstaller.stageBundledComponents(getApplication(), imageFs)
            }
            return false
        }

        _isInstalling.value = true
        _progress.value = 0
        _step.value = 0
        _status.value = "Installing system files"
        _degraded.value = false
        _showProceed.value = false

        ImageFsInstaller.installFromAssetsWithCallback(
            activity,
            { pct ->
                // ImageFS is the first of five steps.
                _progress.value = (pct.coerceIn(0, 100) * 20) / 100
            },
            {
                _progress.value = 20
                viewModelScope.launch { installFirstRunContents() }
            },
        )
        return true
    }

    /**
     * One-time component setup after a fresh ImageFS extract: Proton, DXVK, FEXCore and VKD3D,
     * pinned to the exact Official-catalog builds, installed sequentially behind the same overlay.
     * Failures are counted, not fatal — the user can retry from Contents later.
     */
    private suspend fun installFirstRunContents() {
        var failures = 0
        val app = getApplication<Application>()

        FIRST_RUN_CONTENTS.forEachIndexed { index, item ->
            _step.value = index + 1
            _status.value = "${item.label} — starting"
            val ok = ContentsInstaller.installAwait(app, item.url) { message ->
                _status.value = "${item.label} — $message"
                PERCENT.find(message)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { pct ->
                    _progress.value = 20 + index * 20 + (pct.coerceIn(0, 100) * 20) / 100
                }
            }
            if (!ok) failures++
            _progress.value = 20 + (index + 1) * 20
        }

        _step.value = FIRST_RUN_CONTENTS.size + 1
        _progress.value = 100
        _degraded.value = failures > 0
        _status.value =
            if (failures == 0) "Ready to play"
            else "$failures component(s) failed — retry from Contents"
        _showProceed.value = true
    }

    /**
     * Settings → Reinstall ImageFS. Reuses the same overlay, but deliberately does NOT reinstall
     * the components: those are first-open only.
     */
    fun reinstallImageFs(activity: MainActivity) {
        if (_isInstalling.value) return
        _isInstalling.value = true
        _progress.value = 0
        _step.value = 0
        _status.value = "Reinstalling system files"
        _degraded.value = false
        _showProceed.value = false

        ImageFsInstaller.installFromAssetsWithCallback(
            activity,
            { pct -> _progress.value = pct.coerceIn(0, 100) },
            {
                _progress.value = 100
                _step.value = FIRST_RUN_CONTENTS.size + 1
                _status.value = "System files reinstalled"
                _showProceed.value = true
            },
        )
    }

    /** Called when user taps Proceed; hides the splash overlay. */
    fun dismissSplash() {
        _showProceed.value = false
        _isInstalling.value = false
    }

    private data class FirstRunContent(val label: String, val url: String)

    private companion object {
        val PERCENT = Regex("(\\d+)%")

        /**
         * Pinned from the Official catalog (winlator-contents/contents.json). All four are the
         * arm64ec/PPA-unix builds matching Proton 9 arm64ec.
         */
        val FIRST_RUN_CONTENTS = listOf(
            FirstRunContent(
                label = "Proton 9 arm64ec",
                url = "https://github.com/The412Banner/Nightlies/releases/download/Proton/wine/proton-9.0-arm64ec.wcp",
            ),
            FirstRunContent(
                label = "DXVK 2.4.1 gplasync",
                url = "https://github.com/The412Banner/Nightlies/releases/download/Dxvk-gplasync-arm64ec/dxvk-gplasync-arm64ec-2.4.1-1.wcp",
            ),
            FirstRunContent(
                label = "FEXCore 2610 PPA unix",
                url = "https://github.com/The412Banner/Nightlies/releases/download/FexCore/FEXCore-2610-PPA-unix.wcp",
            ),
            FirstRunContent(
                label = "VKD3D 3.0.1",
                url = "https://github.com/The412Banner/Nightlies/releases/download/VKD3D-PROTON-ARM/vkd3d-proton-arm64ec-3.0.1.wcp",
            ),
        )
    }
}
