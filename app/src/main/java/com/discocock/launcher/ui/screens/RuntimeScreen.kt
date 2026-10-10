package com.discocock.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.discocock.launcher.game.RUNTIME_CATALOG
import com.discocock.launcher.game.RuntimeCatalogEntry
import com.discocock.launcher.game.RuntimeManager
import kotlinx.coroutines.launch

/**
 * Java runtime manager: downloads and extracts JRE packages (.zip/.tar.xz).
 * Default catalog points at PojavLauncherTeam's openjdk builds for Android;
 * any custom URL works too.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuntimeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val runtimes by container.runtimes.runtimes.collectAsState()
    val rtProgress by container.runtimes.progress.collectAsState()
    val activeDownloads by container.downloads.active.collectAsState()
    var showCustomDialog by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Java Runtime") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                "Oyunu başlatmak için bir JRE paketi gerekir. Aşağıdaki hazır girişler " +
                    "PojavLauncherTeam'in Android openjdk derlemelerini indirir; kendi bağlantını da ekleyebilirsin.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { showCustomDialog = true }) { Text("+ Özel URL ile kur") }
            Spacer(Modifier.height(4.dp))

            when (val rp = rtProgress) {
                is RuntimeManager.RuntimeProgress.Working -> {
                    if (rp.total > 0) {
                        LinearProgressIndicator(
                            progress = { rp.downloaded.toFloat() / rp.total.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(
                        "Runtime — ${rp.stage} — ${rp.downloaded / (1024 * 1024)}/${rp.total / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                is RuntimeManager.RuntimeProgress.Failed -> {
                    Text(rp.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                is RuntimeManager.RuntimeProgress.Done -> {
                    Text(rp.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                else -> Unit
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Text("Kurulu runtime'lar", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(runtimes, key = { it.name }) { runtime ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(runtime.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    (if (runtime.version.isBlank()) "Java" else runtime.version) +
                                        " · " + runtime.javaBinary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { scope.launch { container.runtimes.remove(runtime.name) } }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Hazır katalog", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RUNTIME_CATALOG, key = { it.name }) { entry ->
                    val downloading = activeDownloads.any { it.id == "runtime:${entry.name}" || it.id == "runtime:auto" }
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    entry.version,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            when {
                                downloading -> TextButton(
                                    onClick = { container.downloads.cancelAll() },
                                ) { Text("Durdur") }
                                runtimes.none { it.name == entry.name } -> TextButton(
                                    onClick = { container.downloads.installRuntime(entry) },
                                ) { Text("Kur") }
                                else -> Text(
                                    "Kurulu",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        CustomRuntimeDialog(
            onDismiss = { showCustomDialog = false },
            onInstall = { entry ->
                showCustomDialog = false
                container.downloads.installRuntime(entry)
            },
        )
    }
}

@Composable
private fun CustomRuntimeDialog(onDismiss: () -> Unit, onInstall: (RuntimeCatalogEntry) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var javaBinary by remember { mutableStateOf("bin/java") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Özel JRE paketi") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ad") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL (.zip veya .tar.xz)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = javaBinary,
                    onValueChange = { javaBinary = it },
                    label = { Text("java yolu (paket içi)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && url.isNotBlank(),
                onClick = {
                    onInstall(
                        RuntimeCatalogEntry(
                            name = name.trim(),
                            url = url.trim(),
                            javaBinary = javaBinary.trim().ifBlank { "bin/java" },
                        ),
                    )
                },
            ) { Text("İndir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

