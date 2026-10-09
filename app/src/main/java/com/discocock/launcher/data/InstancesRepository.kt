package com.discocock.launcher.data

import android.content.Context
import com.discocock.launcher.util.JsonFiles
import com.discocock.launcher.util.LauncherPaths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class Instance(
    val id: String,
    val name: String,
    val versionId: String,
    val icon: String = "minecraft",
    val javaMemoryMb: Int = 2048,
    val serverAddress: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
private data class InstanceStore(val instances: List<Instance> = emptyList())

class InstancesRepository(private val context: Context, private val paths: LauncherPaths) {

    private val file = File(context.filesDir, "instances.json")
    private val mutex = Mutex()

    private val _instances = MutableStateFlow<List<Instance>>(emptyList())
    val instances: StateFlow<List<Instance>> = _instances.asStateFlow()

    val gamesRoot: File get() = paths.root

    suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val store = JsonFiles.read(file, InstanceStore())
                _instances.value = store.instances.sortedBy { it.createdAt }
            }
        }
    }

    suspend fun create(name: String, versionId: String): Result<Instance> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Instance adı boş olamaz"))
        }
        val instance = Instance(
            id = "inst-" + System.currentTimeMillis().toString(36),
            name = trimmed,
            versionId = versionId,
        )
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val store = JsonFiles.read(file, InstanceStore())
                val next = InstanceStore(store.instances + instance)
                JsonFiles.write(file, next)
                _instances.value = next.instances.sortedBy { it.createdAt }
            }
        }
        return Result.success(instance)
    }

    suspend fun update(updated: Instance) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val store = JsonFiles.read(file, InstanceStore())
                val next = InstanceStore(store.instances.map { if (it.id == updated.id) updated else it })
                JsonFiles.write(file, next)
                _instances.value = next.instances.sortedBy { it.createdAt }
            }
        }
    }

    suspend fun delete(id: String) {
        withContext(Dispatchers.IO) {
            val dir = File(paths.root, id)
            dir.deleteRecursively()
            mutex.withLock {
                val store = JsonFiles.read(file, InstanceStore())
                val next = InstanceStore(store.instances.filterNot { it.id == id })
                JsonFiles.write(file, next)
                _instances.value = next.instances.sortedBy { it.createdAt }
            }
        }
    }

    fun instanceDir(instance: Instance): File = File(paths.root, instance.id)

    /** True when the vanilla client jar for this instance's version exists. */
    fun isInstalled(instance: Instance): Boolean =
        File(paths.versionsDir, instance.versionId + "/" + instance.versionId + ".jar").exists()
}
