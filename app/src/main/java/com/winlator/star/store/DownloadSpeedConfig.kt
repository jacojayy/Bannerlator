package com.winlator.star.store

/**
 * Depot-download concurrency derived from CPU cores × per-tier ratios.
 *
 * Faithful port of GameNative's `DownloadSpeedConfig` (which runs the *same*
 * `in.dragonbra:javasteam-depotdownloader` engine we do). The engine models a download as a
 * three-stage pipeline — network fetch → decompress → file write — each stage bounded
 * independently. Only the decompress and file-write stages hold large (~chunk-uncompressed,
 * up to several MB) buffers, so those are the heap drivers; network parallelism is cheap on heap.
 *
 * Tiers are keyed by an integer value so the value can travel through
 * [SteamDepotDownloader.installApp]/[SteamDepotDownloader.resumeApp] and the picker UI as a
 * single [Int]:
 *
 *   8  = Slow     (0.6 dl / 0.2 dc)
 *   16 = Medium   (1.2 dl / 0.4 dc)
 *   24 = Fast     (1.5 dl / 0.5 dc)   <-- default
 *   32 = Blazing  (2.4 dl / 0.8 dc)
 *
 * `maxDownloads = (cores * download).coerceAtLeast(1)`
 * `maxDecompress = (cores * decompress).coerceAtLeast(1)`
 *
 * [maxFileWrites] is NOT user-exposed (GameNative omits it and relies on the engine default).
 * Our positional 9-arg constructor requires the argument, so we tie it to [maxDecompress] to keep
 * the peak count of simultaneously-live large buffers bounded (~maxDecompress + maxFileWrites).
 */
class DownloadSpeedConfig(private val tier: Int) {

    private data class Ratios(val download: Double, val decompress: Double)

    companion object {
        const val TIER_SLOW = 8
        const val TIER_MEDIUM = 16
        const val TIER_FAST = 24
        const val TIER_BLAZING = 32

        /** App default = Fast (matches GameNative's default tier). */
        const val DEFAULT_TIER = TIER_FAST
    }

    private val ratios: Ratios
        get() = when (tier) {
            TIER_SLOW -> Ratios(download = 0.6, decompress = 0.2)
            TIER_MEDIUM -> Ratios(download = 1.2, decompress = 0.4)
            TIER_FAST -> Ratios(download = 1.5, decompress = 0.5)
            TIER_BLAZING -> Ratios(download = 2.4, decompress = 0.8)
            // Unknown/corrupt value → behave as the app default (Fast), not GameNative's Slow
            // fallback, so a stray tier never silently throttles the default experience.
            else -> Ratios(download = 1.5, decompress = 0.5)
        }

    val cpuCores: Int
        get() = Runtime.getRuntime().availableProcessors()

    val maxDownloads: Int
        get() = (cpuCores * ratios.download).toInt().coerceAtLeast(1)

    val maxDecompress: Int
        get() = (cpuCores * ratios.decompress).toInt().coerceAtLeast(1)

    /**
     * File-write stage concurrency. Tied to [maxDecompress] so peak live large buffers stay
     * bounded. GameNative doesn't expose this; our positional ctor requires it, so we derive it.
     */
    val maxFileWrites: Int
        get() = maxDecompress
}
