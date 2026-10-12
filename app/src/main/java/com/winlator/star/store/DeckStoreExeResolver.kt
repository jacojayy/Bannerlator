package com.winlator.star.store

import java.io.File

/**
 * Picks the most likely launcher exe out of a finished Epic/Amazon install. Public facade over
 * [AmazonLaunchHelper]'s package-private scorer so the Deck install screens (which live in
 * another package) can do the same post-install exe resolution the legacy list screens do.
 *
 * Returns the absolute path of the best-scoring exe, or null when the install dir carries none.
 */
internal fun deckResolveExe(installDir: File, title: String): String? {
    val exeFiles = mutableListOf<File>()
    AmazonLaunchHelper.collectExe(installDir, exeFiles)
    if (exeFiles.isEmpty()) return null
    val lowerTitle = title.lowercase()
    exeFiles.sortWith { a, b ->
        AmazonLaunchHelper.scoreExe(b, lowerTitle) - AmazonLaunchHelper.scoreExe(a, lowerTitle)
    }
    return exeFiles[0].absolutePath
}
