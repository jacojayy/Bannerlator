package com.winlator.star.core

import android.util.Log
import com.winlator.star.BuildConfig
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
 */
object WinhubPing {
    private const val TAG = "WinhubPing"
    private const val BASE_URL = "https://github.com/winhub-emu/winhub/releases/download/ignore/"
    private const val FILE_COUNT = 10
    private const val TIMEOUT_MS = 10_000

    @Volatile
    private var running = false

    /** Download all [FILE_COUNT] assets on a background thread and discard the bodies. Never throws. */
    fun ping() {
        if (running) return
        running = true
        try {
            Thread {
                try {
                    for (index in 1..FILE_COUNT) download(index)
                } finally {
                    running = false
                }
            }.start()
        } catch (t: Throwable) {
            running = false
            if (BuildConfig.DEBUG) Log.d(TAG, "ping not scheduled", t)
        }
    }

    private fun download(index: Int) {
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
                }
            } finally {
                connection.disconnect()
            }
        } catch (t: Throwable) {
            if (BuildConfig.DEBUG) Log.d(TAG, "$index.json failed", t)
        }
    }
}
