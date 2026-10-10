package com.discocock.launcher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.game.MrpackInstaller
import com.discocock.launcher.net.ModrinthProject
import com.discocock.launcher.net.ModrinthVersion
import com.discocock.launcher.util.NetFiles
import kotlinx.coroutines.launch
import java.io.File

/**
 * Modpack store — Modrinth public API (no key needed), discovery list with
 * project icons, badges and background installs like the desktop store.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val instances by container.instances.instances.collectAsState()
    val mrpackProgress by container.mrpackInstaller.progress.collectAsState()
    val activeDownloads by container.downloads.active.collectAsState()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<ModrinthProject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var resultCount by remember { mutableStateOf(0) }

    var selectedProject by remember { mutableStateOf<ModrinthProject?>(null) }
    var projectVersions by remember { mutableStateOf<List<ModrinthVersion>>(emptyList()) }
    var versionsLoading by remember { mutableStateOf(false) }

    var installTarget by remember { mutableStateOf<ModrinthVersion?>(null) }

    fun search(q: String) {
        scope.launch {
            loading = true
            error = null
            val result = runCatching { container.modrinth.searchModpacks(q) }
            results = result.getOrDefault(emptyList())
            resultCount = results.size
            error = result.exceptionOrNull()?.message
            loading = false
        }
    }

    // Popular packs load immediately (empty query = relevance order).
    LaunchedEffect(Unit) { search("") }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Mağaza", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Modrinth mod paketleri — anahtar gerekmez, telemetri yok",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Mod paketi ara") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; search("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Temizle")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { search(query) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Aranıyor…", style = MaterialTheme.typography.bodySmall)
            } else if (resultCount > 0 && error == null) {
                Text(
                    "$resultCount sonuç" + (if (query.isBlank()) " · popüler" else " · \"$query\""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val mp = mrpackProgress
        when (mp) {
            is MrpackInstaller.MrpackProgress.Working -> {
                LinearProgressIndicator(
                    progress = { if (mp.total <= 0) 0f else mp.current.toFloat() / mp.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Mod paketi — ${mp.stage} ${mp.current}/${mp.total}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
            }
            is MrpackInstaller.MrpackProgress.Done -> {
                Text(mp.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            is MrpackInstaller.MrpackProgress.Failed -> {
                Text(mp.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }
            else -> Unit
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results, key = { it.id }) { project ->
                ProjectCard(
                    project = project,
                    onClick = {
                        selectedProject = project
                        scope.launch {
                            versionsLoading = true
                            val result = runCatching { container.modrinth.versions(project.id) }
                            projectVersions = result.getOrDefault(emptyList())
                            versionsLoading = false
                        }
                    },
                )
            }
        }
    }

    selectedProject?.let { project ->
        AlertDialog(
            onDismissRequest = { selectedProject = null },
            title = { Text(project.title) },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = project.iconUrl,
                            contentDescription = project.title,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(project.slug, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${formatCount(project.downloads)} indirme",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
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
                                        "MC " + version.gameVersions.take(3).joinToString(", "),
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
                                selectedProject = null
                                scope.launch {
                                    try {
                                        val dest = File(
                                            File(container.instances.instanceDir(instance), "modpacks"),
                                            file.filename,
                                        )
                                        NetFiles.download(file.url, dest)
                                        // Kurulum arka planda sürer; Mağaza'ndan ayrılsan da devam eder
                                        container.downloads.installModpack(dest, container.instances.instanceDir(instance), instance.name)
                                    } catch (e: Exception) {
                                        error = "İndirme hatası: ${e.message}"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(project: ModrinthProject, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = project.iconUrl,
                contentDescription = project.title,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Column(Modifier.weight(1f)) {
                Text(
                    project.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    project.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            "MOD PACK",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Download,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatCount(project.downloads),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 1234567 -> "1,2M", 45000 -> "45K" */
private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format(java.util.Locale.getDefault(), "%.1fM", count / 1_000_000f)
    count >= 1_000 -> String.format(java.util.Locale.getDefault(), "%.0fK", count / 1_000f)
    else -> count.toString()
}
