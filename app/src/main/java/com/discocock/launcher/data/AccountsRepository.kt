package com.discocock.launcher.data

import android.content.Context
import com.discocock.launcher.util.JsonFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.util.UUID

@Serializable
data class Account(
    val id: String,
    val type: String, // "offline" | "microsoft"
    val username: String,
    val uuid: String,
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAt: Long = 0L,
)

@Serializable
private data class AccountStore(
    val accounts: List<Account> = emptyList(),
    val activeId: String? = null,
)

/** JSON-file-backed account store: offline + Microsoft, like the desktop launcher. */
class AccountsRepository(context: Context) {

    private val file = File(context.filesDir, "accounts.json")
    private val mutex = Mutex()

    private val _accounts = MutableStateFlow<List<Account>>(emptyList())
    val accounts: StateFlow<List<Account>> = _accounts.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    suspend fun load() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val store = JsonFiles.read(file, AccountStore())
                _accounts.value = store.accounts
                _activeId.value = store.activeId
            }
        }
    }

    suspend fun addOffline(username: String): Result<Account> {
        val name = username.trim()
        if (name.isEmpty()) {
            return Result.failure(IllegalArgumentException("Kullanıcı adı boş olamaz"))
        }
        // Deterministic offline UUID, exactly like a vanilla offline-mode server.
        val uuid = UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray()).toString()
        val account = Account(id = "offline:$name", type = TYPE_OFFLINE, username = name, uuid = uuid)
        upsert(account)
        return Result.success(account)
    }

    suspend fun upsert(account: Account) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = JsonFiles.read(file, AccountStore())
                val next = current.copy(
                    accounts = current.accounts.filterNot { it.id == account.id } + account,
                )
                JsonFiles.write(file, next)
                _accounts.value = next.accounts
                if (next.activeId == null || next.activeId == account.id) {
                    setActiveLocked(next, account.id)
                }
            }
        }
    }

    suspend fun remove(id: String) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = JsonFiles.read(file, AccountStore())
                val next = current.copy(accounts = current.accounts.filterNot { it.id == id })
                JsonFiles.write(file, next)
                _accounts.value = next.accounts
                if (current.activeId == id) {
                    setActiveLocked(next, next.accounts.firstOrNull()?.id)
                }
            }
        }
    }

    suspend fun setActive(id: String?) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val current = JsonFiles.read(file, AccountStore())
                setActiveLocked(current, id)
            }
        }
    }

    fun activeAccount(): Account? = _accounts.value.firstOrNull { it.id == _activeId.value }

    private fun setActiveLocked(current: AccountStore, id: String?) {
        val next = current.copy(activeId = id)
        JsonFiles.write(file, next)
        _activeId.value = id
    }

    companion object {
        const val TYPE_OFFLINE = "offline"
        const val TYPE_MICROSOFT = "microsoft"
    }
}
