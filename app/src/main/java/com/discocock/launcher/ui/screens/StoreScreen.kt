package com.discocock.launcher.ui.screens

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.discocock.launcher.net.ModrinthProject
import com.discocock.launcher.net.ModrinthVersion
import kotlinx.coroutines.launch

/**
 * Modpack store — Modrinth public API (no key needed), discovery grid like
 * the desktop launcher's store view.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val instances by container.instances.instances.collectAsState()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ModrinthProject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var selectedProject by remember { mutableStateOf<ModrinthProject?>(null) }
    var projectVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var versionsLoading by remember { mutableStateOf(false) }

    var installTarget by remember { mutableStateOf<ModrinthVersion?>(null) }
    var installProgress by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var installMessage by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Mağaza", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Modrinth mod paketleri — anahtar gerekmez",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Mod paketi ara") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    scope.launch {
                        loading = true
                        error = null
                        val result = runCatching { container.modrinth.searchModpacks(query) }
                        results = result.getOrDefault(emptyList())
                        error = result.exceptionOrNull()?.message
                        loading = false
                    }
                },
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Ara", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(10.dp))

        installProgress?.let { (current, total) ->
            LinearProgressIndicator(
                progress = { if (total <= 0) 0f else current.toFloat() / total },
                modifier = Modifier.fillMaxWidth(),
            )
            installMessage?.let {
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        if (loading) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Aranıyor…")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.id }) { project ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        selectedProject = project
                        installMessage = null
                        scope.launch {
                            versionsLoading = true
                            val result = runCatching { container.modrinth.versions(project.id) }
                            projectVersions = result.getOrDefault(emptyList())
                            versionsLoading = false
                        }
                    },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(project.title, style = MaterialTheme.typography.titleSmall)
                        Text(
                            project.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${project.downloads} indirme",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    selectedProject?.let { project ->
        AlertDialog(
            onDismissRequest = { selectedProject = null },
            title = { Text(project.title) },
            text = {
                Column {
                    Text(project.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    if (versionsLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Sürümler yükleniyor…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(projectVersions, key = { it.id }) { version ->
                            val primaryFile = version.files.firstOrNull { it.primary }
                                ?: version.files.firstOrNull()
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(version.versionNumber, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        version.gameVersions.take(3).joinToString(", "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(
                                    enabled = primaryFile != null && instances.isNotEmpty(),
                                    onClick = { installTarget = version },
                                ) { Text("Kur") }
                            }
                        }
                    }
                    if (instances.isEmpty()) {
                        Text(
                            "Önce Ana Ekranda bir instance oluştur.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedProject = null }) { Text("Kapat") }
            },
        )
    }

    installTarget?.let { version ->
        AlertDialog(
            onDismissRequest = { installTarget = null },
            title = { Text("Hangi instance'a kurulsun?") },
            text = {
                Column {
                    if (instances.isEmpty()) {
                        Text("Instance yok.", color = MaterialTheme.colorScheme.error)
                    }
                    instances.forEach { instance ->
                        TextButton(onClick = {
                            val file = version.files.firstOrNull { it.primary }
                                ?: version.files.firstOrNull()
                            if (file != null) {
                                installTarget = null
                                scope.launch {
                                    installProgress = 0 to 1
                                    installMessage = "İndiriliyor: ${file.filename}"
                                    try {
                                        val dest = java.io.File(
                                            container.instances.instanceDir(instance),
                                            "modpacks/" + file.filename,
                                        )
                                        com.discocock.launcher.util.NetFiles.download(file.url, dest)
                                        installMessage = "Kuruluyor: ${instance.name}"
                                        val count = container.mrpackInstaller.install(
                                            dest,
                                            container.instances.instanceDir(instance),
                                        ) { _, current, total ->
                                            installProgress = current to total.coerceAtLeast(1)
                                        }
                                        installProgress = null
                                        installMessage = if (count.isSuccess) {
                                            "Kurulum tamam: ${instance.name} (${count.getOrDefault(0)} dosya)"
                                        } else {
                                            "Hata: " + (count.exceptionOrNull()?.message ?: "bilinmeyen")
                                        }
                                    } catch (e: Exception) {
                                        installProgress = null
                                        installMessage = "Hata: ${e.message}"
                                    }
                                }
                            }
                        }) {
                            Text("${instance.name} (${instance.versionId})")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { installTarget = null }) { Text("İptal") }
            },
        )
    }
}
