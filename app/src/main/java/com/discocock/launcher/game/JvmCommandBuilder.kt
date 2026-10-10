package com.discocock.launcher.game

import com.discocock.launcher.data.Account
import com.discocock.launcher.data.Instance
import java.io.File

data class GameLaunchSpec(
    val command: List<String>,
    val workingDir: File,
)

/**
 * Builds the full JVM command line for a vanilla launch, mirroring what the
 * desktop launcher's runtime does: classpath = client jar + libraries,
 * memory flags, asset paths, account credentials.
 */
object JvmCommandBuilder {

    fun build(
        javaBin: File,
        instance: Instance,
        info: InstalledVersionInfo,
        versionsDir: File,
        assetsDir: File,
        librariesDir: File,
        instanceDir: File,
        account: Account,
        extraJvmArgs: List<String> = emptyList(),
    ): GameLaunchSpec {
        val clientJar = File(File(versionsDir, instance.versionId), instance.versionId + ".jar")
        val libraryJars = librariesDir.walkTopDown()
            .filter { it.isFile && it.extension == "jar" }
            .map { it.absolutePath }
            .toList()
        val classpath = (listOf(clientJar.absolutePath) + libraryJars).joinToString(":")

        val memory = instance.javaMemoryMb.coerceAtLeast(512)
        val jvmArgs = buildList {
            add("-Xms${minOf(512, memory)}M")
            add("-Xmx${memory}M")
            add("-Djava.io.tmpdir=${instanceDir.absolutePath}")
            addAll(extraJvmArgs)
        }

        val gameArgs = mutableListOf(
            "--username", account.username,
            "--version", instance.versionId,
            "--gameDir", instanceDir.absolutePath,
            "--assetsDir", assetsDir.absolutePath,
            "--assetIndex", info.assetIndexId,
            "--uuid", account.uuid,
            "--accessToken", account.accessToken.ifBlank { "0" },
            "--userType", if (account.type == Account.TYPE_MICROSOFT) "msa" else "legacy",
        )
        if (instance.serverAddress.isNotBlank()) {
            gameArgs += listOf("--server", instance.serverAddress)
        }

        val command = buildList {
            add(javaBin.absolutePath)
            addAll(jvmArgs)
            add("-cp")
            add(classpath)
            add(info.mainClass)
            addAll(gameArgs)
        }
        return GameLaunchSpec(command = command, workingDir = instanceDir)
    }
}
