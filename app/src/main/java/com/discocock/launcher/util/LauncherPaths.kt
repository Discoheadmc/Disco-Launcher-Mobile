package com.discocock.launcher.util

import android.content.Context
import java.io.File

/**
 * Central file layout. Everything lives under the app's external files dir
 * (app-private, no permission needed) so the game process and file managers
 * can be pointed at a single root, like the desktop launcher's instances dir.
 */
class LauncherPaths(context: Context) {

    private val base: File = context.getExternalFilesDir(null) ?: context.filesDir

    val root: File get() = File(base, "DiscoLauncher")
    val versionsDir: File get() = File(root, "versions")
    val assetsDir: File get() = File(root, "assets")
    val librariesDir: File get() = File(root, "libraries")
    val runtimesDir: File get() = File(root, "runtimes")
    val modpacksDir: File get() = File(root, "modpacks")
}
