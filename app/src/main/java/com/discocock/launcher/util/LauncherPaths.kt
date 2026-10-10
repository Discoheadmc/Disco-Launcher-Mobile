package com.discocock.launcher.util

import android.content.Context
import java.io.File

/**
 * Central file layout. Game data lives under the app's external files dir
 * (app-private, no permission needed). The Java runtime is deliberately kept
 * in INTERNAL storage: emulated external storage is mounted noexec on
 * Android 10+, which would prevent the java binary from running.
 */
class LauncherPaths(context: Context) {

    private val base: File = context.getExternalFilesDir(null) ?: context.filesDir
    private val internal: File = context.filesDir

    val root: File get() = File(base, "DiscoLauncher")
    val versionsDir: File get() = File(root, "versions")
    val assetsDir: File get() = File(root, "assets")
    val librariesDir: File get() = File(root, "libraries")
    val modpacksDir: File get() = File(root, "modpacks")

    /** JRE packages — internal storage so the java binary can be exec'd. */
    val runtimesDir: File get() = File(internal, "runtimes")
}
