package com.discocock.launcher.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.net.DevicePollResult
import kotlinx.coroutines.launch

@Composable
fun AccountsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val accounts by container.accounts.accounts.collectAsState()
    val activeId by container.accounts.activeId.collectAsState()
    val clientId by container.settings.microsoftClientId.collectAsState(initial = "")

    var showOfflineDialog by remember { mutableStateOf(false) }
    var showMicrosoftDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                ExtendedFloatingActionButton(
                    onClick = { showOfflineDialog = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Çevrimdışı") },
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        if (clientId.isBlank()) {
                            error = "Microsoft girişi için Ayarlar'da bir client_id gir (ücretsiz Azure uygulaması)."
                        } else {
                            showMicrosoftDialog = true
                        }
                    },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    text = { Text("Microsoft") },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("Hesaplar", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Microsoft + çevrimdışı — masaüstü Disco Launcher gibi",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.id }) { account ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { scope.launch { container.accounts.setActive(account.id) } },
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(account.username, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    (if (account.type == "microsoft") "Microsoft" else "Çevrimdışı") +
                                        if (account.id == activeId) " · aktif" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { scope.launch { container.accounts.remove(account.id) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOfflineDialog) {
        OfflineAccountDialog(
            onDismiss = { showOfflineDialog = false },
            onAdd = { name ->
                scope.launch {
                    val result = container.accounts.addOffline(name)
                    if (result.isFailure) {
                        error = result.exceptionOrNull()?.message
                    } else {
                        container.accounts.setActive(result.getOrNull()?.id)
                        showOfflineDialog = false
                    }
                }
            },
        )
    }

    if (showMicrosoftDialog) {
        MicrosoftLoginDialog(
            onDismiss = { showMicrosoftDialog = false },
        )
    }
}

@Composable
private fun OfflineAccountDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Çevrimdışı hesap") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Kullanıcı adı") },
                singleLine = true,
            )
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = { onAdd(name) }) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

@Composable
private fun MicrosoftLoginDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()
    val clientId by container.settings.microsoftClientId.collectAsState(initial = "")

    var userCode by remember { mutableStateOf<String?>(null) }
    var verificationUrl by remember { mutableStateOf<String?>(null) }
    var deviceCode by remember { mutableStateOf<String?>(null) }
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var polling by remember { mutableStateOf(false) }

    // Start the device-code flow once, when the dialog opens.
    LaunchedEffect(Unit) {
        try {
            val start = container.microsoftAuth.startDeviceCode(clientId)
            userCode = start.userCode
            verificationUrl = start.verificationUrl
            deviceCode = start.deviceCode
            polling = true
        } catch (e: Exception) {
            error = e.message ?: "Device code alınamadı"
        }
    }

    // Poll while active.
    LaunchedEffect(polling, deviceCode) {
        val code = deviceCode ?: return@LaunchedEffect
        if (!polling) return@LaunchedEffect
        while (polling && !done) {
            when (val result = container.microsoftAuth.pollDeviceCode(clientId, code)) {
                is DevicePollResult.Pending -> {
                    waiting = true
                    kotlinx.coroutines.delay(3000)
                }
                is DevicePollResult.Expired -> {
                    error = "Kod süresi doldu — tekrar deneyin"
                    polling = false
                }
                is DevicePollResult.Error -> {
                    error = result.message
                    polling = false
                }
                is DevicePollResult.Success -> {
                    waiting = false
                    polling = false
                    try {
                        val account = container.microsoftAuth.completeLogin(
                            result.accessToken,
                            result.refreshToken,
                        )
                        container.accounts.upsert(account)
                        container.accounts.setActive(account.id)
                        done = true
                    } catch (e: Exception) {
                        error = e.message ?: "Giriş tamamlanamadı"
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Microsoft ile giriş") },
        text = {
            Column {
                if (done) {
                    Text("Giriş başarılı! Hesap eklendi.", color = MaterialTheme.colorScheme.primary)
                } else if (userCode != null) {
                    Text("1. Telefondan veya bilgisayardan şu adresi aç:")
                    Text(
                        verificationUrl ?: "https://www.microsoft.com/link",
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("2. Bu kodu girin:")
                    Text(userCode ?: "", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    if (waiting) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Onay bekleniyor…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else if (error == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Bağlanıyor…")
                    }
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (done) "Kapat" else "İptal") }
        },
    )
}
