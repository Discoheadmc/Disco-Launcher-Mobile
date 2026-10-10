package com.discocock.launcher.di

import android.content.Context
import com.discocock.launcher.data.AccountsRepository
import com.discocock.launcher.data.InstancesRepository
import com.discocock.launcher.data.SettingsRepository
import com.discocock.launcher.game.GameInstaller
import com.discocock.launcher.game.GameLauncher
import com.discocock.launcher.game.MrpackInstaller
import com.discocock.launcher.game.RuntimeManager
import com.discocock.launcher.net.MicrosoftAuth
import com.discocock.launcher.net.MojangApi
import com.discocock.launcher.net.ModrinthApi
import com.discocock.launcher.util.LauncherPaths

/** Simple manual DI container; owned by [com.discocock.launcher.DiscoApplication]. */
class AppContainer(context: Context) {

    val paths = LauncherPaths(context)

    val settings = SettingsRepository(context)
    val accounts = AccountsRepository(context)
    val instances = InstancesRepository(context, paths)
    val mojang = MojangApi()
    val modrinth = ModrinthApi()
    val microsoftAuth = MicrosoftAuth()
    val runtimes = RuntimeManager(paths)
    val launcher = GameLauncher(paths)
    val installer = GameInstaller(paths, mojang)
    val mrpackInstaller = MrpackInstaller()
    val downloads = DownloadManager(installer, runtimes, mrpackInstaller)
}
