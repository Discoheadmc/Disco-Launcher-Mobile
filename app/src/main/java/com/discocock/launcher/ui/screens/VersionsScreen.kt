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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.discocock.launcher.game.GameInstaller
import com.discocock.launcher.net.ManifestVersion
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val progress by container.installer.progress.collectAsState()
    var versions by remember { mutableStateOf<List<ManifestVersion>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSnapshots by remember { mutableStateOf(false) }
    var includeAssets by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        loading = true
        val result = runCatching { container.mojang.versionManifest() }
        versions = result.getOrDefault(emptyList())
        error = result.exceptionOrNull()?.message
        loading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sürüm İndir") },
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { showSnapshots = !showSnapshots },
                    label = { Text(if (showSnapshots) "Snapshot: açık" else "Snapshot: kapalı") },
                )
                AssistChip(
                    onClick = { includeAssets = !includeAssets },
                    label = { Text(if (includeAssets) "Varlıklar: açık" else "Varlıklar: kapalı") },
                )
            }
            Spacer(Modifier.height(8.dp))

            val p = progress
            if (p is GameInstaller.Progress.Working) {
                LinearProgressIndicator(
                    progress = {
                        if (p.total <= 0) 0f else p.current.toFloat() / p.total.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${p.stage} — ${p.current}/${p.total}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            } else if (p is GameInstaller.Progress.Done) {
                Text(p.message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            if (loading) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Manifest yükleniyor…")
                }
            }
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                val visible = versions
                    .filter { showSnapshots || it.type == "release" }
                    .take(200)
                items(visible, key = { it.id }) { version ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            scope.launch {
                                container.installer.installVanilla(
                                    version.id,
                                    versionUrl = version.url,
                                    includeAssets = includeAssets,
                                )
                            }
                        },
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(version.id, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    version.type + " · " + version.releaseTime.take(10),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = "İndir",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
