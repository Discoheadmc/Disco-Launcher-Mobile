package com.discocock.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.data.Instance
import com.discocock.launcher.game.GameInstaller
import com.discocock.launcher.game.JvmCommandBuilder
import com.discocock.launcher.net.ManifestVersion
import com.discocock.launcher.ui.theme.LocalLaunchButtonColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onOpenVersions: () -> Unit, onOpenRuntime: () -> Unit) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val instances by container.instances.instances.collectAsState()
    val accounts by container.accounts.accounts.collectAsState()
    val activeId by container.accounts.activeId.collectAsState()
    val installProgress by container.installer.progress.collectAsState()
    val launchState by container.launcher.state.collectAsState()
    val launchButtonColor = LocalLaunchButtonColor.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var detailInstance by remember { mutableStateOf<Instance?>(null) }
    var launchError by remember { mutableStateOf<String?>(null) }
    val activeAccount = accounts.firstOrNull { it.id == activeId }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Instance") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("Disco Launcher", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = activeAccount?.let {
                    "Aktif: ${it.username} (${if (it.type == "microsoft") "Microsoft" else "Çevrimdışı"})"
                } ?: "Hesap eklenmemiş — Hesaplar sekmesinden ekle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = onOpenVersions, label = { Text("Sürüm İndir") })
                AssistChip(onClick = onOpenRuntime, label = { Text("Java Runtime") })
            }
            Spacer(Modifier.height(10.dp))

            val progress = installProgress
            when (progress) {
                is GameInstaller.Progress.Working -> {
                    LinearProgressIndicator(
                        progress = {
                            if (progress.total <= 0) 0f
                            else progress.current.toFloat() / progress.total.toFloat()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "${progress.stage} — ${progress.current}/${progress.total}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                is GameInstaller.Progress.Failed -> {
                    Text(
                        "Kurulum hatası: ${progress.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                else -> Unit
            }

            if (instances.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "Henüz instance yok.\nSağ alttaki + ile oluştur.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(instances, key = { it.id }) { instance ->
                        InstanceCard(
                            instance = instance,
                            installed = container.instances.isInstalled(instance),
                            onClick = { detailInstance = instance },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateInstanceDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, versionId ->
                scope.launch {
                    container.instances.create(name, versionId)
                    showCreateDialog = false
                }
            },
        )
    }

    detailInstance?.let { instance ->
        InstanceDetailDialog(
            instance = instance,
            installed = container.instances.isInstalled(instance),
            launchButtonColor = launchButtonColor,
            onDismiss = { detailInstance = null },
            onLaunch = {
                detailInstance = null
                scope.launch { launchHomeInstance(container, instance) { msg -> launchError = msg } }
            },
            onInstall = {
                scope.launch {
                    container.installer.installVanilla(instance.versionId, includeAssets = true)
                }
            },
            onDelete = {
                detailInstance = null
                scope.launch { container.instances.delete(instance.id) }
            },
        )
    }

    launchState.let { state ->
        if (state is com.discocock.launcher.game.GameLauncher.LaunchState.Failed) {
            AlertDialog(
                onDismissRequest = { launchError = null; container.launcher.acknowledge() },
                title = { Text("Başlatılamadı") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { container.launcher.acknowledge() }) { Text("Tamam") }
                },
            )
        }
    }

    launchError?.let { message ->
        AlertDialog(
            onDismissRequest = { launchError = null },
            title = { Text("Hata") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { launchError = null }) { Text("Tamam") } },
        )
    }
}

/** Launches the instance: installs the version if needed, then boots the JVM. */
private suspend fun launchHomeInstance(
    container: com.discocock.launcher.di.AppContainer,
    instance: Instance,
    onError: (String) -> Unit,
) {
    val account = container.accounts.activeAccount()
    if (account == null) {
        onError("Önce bir hesap ekle (Hesaplar sekmesi).")
        return
    }
    if (!container.instances.isInstalled(instance)) {
        val result = container.installer.installVanilla(instance.versionId, includeAssets = true)
        if (result.isFailure) {
            onError(result.exceptionOrNull()?.message ?: "Kurulum başarısız")
            return
        }
    }
    val runtime = container.runtimes.activeRuntime()
    if (runtime == null) {
        onError(
            "Java runtime kurulu değil.\n\nAna ekrandaki 'Java Runtime' bölümünden bir JRE paketi kur " +
                "(örn. PojavLauncherTeam openjdk aarch64 derlemesi).",
        )
        return
    }
    val info = container.installer.installedInfo(instance.versionId)
    if (info == null) {
        onError("Kurulum bilgisi bulunamadı — sürümü yeniden kur.")
        return
    }
    val javaBin = container.runtimes.javaBinaryFile(runtime)
    if (!javaBin.exists()) {
        onError("Java binary bulunamadı: ${javaBin.absolutePath}")
        return
    }
    val spec = JvmCommandBuilder.build(
        javaBin = javaBin,
        instance = instance,
        info = info,
        versionsDir = container.paths.versionsDir,
        assetsDir = container.paths.assetsDir,
        librariesDir = container.paths.librariesDir,
        instanceDir = container.instances.instanceDir(instance),
        account = account,
    )
    container.launcher.launch(spec)
}

@Composable
private fun InstanceCard(instance: Instance, installed: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⛏", style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(10.dp))
            Text(instance.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                instance.versionId,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (installed) "Kurulu" else "Kurulmadı",
                style = MaterialTheme.typography.labelSmall,
                color = if (installed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateInstanceDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, versionId: String) -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    var name by remember { mutableStateOf("") }
    var releaseIds by remember { mutableStateOf<List<ManifestVersion>>(emptyList()) }
    var filter by remember { mutableStateOf("") }
    var selectedVersion by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        val manifest = runCatching { container.mojang.versionManifest() }
        releaseIds = manifest.getOrDefault(emptyList())
            .filter { it.type == "release" }
        loading = false
        error = manifest.exceptionOrNull()?.message
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Instance") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Instance adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    label = { Text("Sürüm filtrele") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Sürümler yükleniyor…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                val visible = releaseIds
                    .filter { it.id.contains(filter, ignoreCase = true) }
                    .take(30)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.height(220.dp),
                ) {
                    items(visible, key = { it.id }) { version ->
                        val selected = selectedVersion == version.id
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.clickable { selectedVersion = version.id },
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    version.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && selectedVersion != null,
                onClick = {
                    val version = selectedVersion
                    if (name.isNotBlank() && version != null) {
                        onCreate(name, version)
                    }
                },
            ) { Text("Oluştur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
    )
}

@Composable
private fun InstanceDetailDialog(
    instance: Instance,
    installed: Boolean,
    launchButtonColor: Color,
    onDismiss: () -> Unit,
    onLaunch: () -> Unit,
    onInstall: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val runtimes by container.runtimes.runtimes.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(instance.name) },
        text = {
            Column {
                Text("Sürüm: ${instance.versionId}")
                Text("RAM: ${instance.javaMemoryMb} MB")
                if (instance.serverAddress.isNotBlank()) {
                    Text("Sunucu: ${instance.serverAddress}")
                }
                Text(
                    if (installed) "Durum: kurulu" else "Durum: kurulmadı",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (runtimes.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Java runtime kurulu değil — başlatma için önce Runtime ekranından JRE kur.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onLaunch,
                colors = if (launchButtonColor == Color.Unspecified) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(containerColor = launchButtonColor)
                },
                enabled = runtimes.isNotEmpty(),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Başlat")
            }
        },
        dismissButton = {
            Row {
                if (!installed) {
                    TextButton(onClick = onInstall) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kur")
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onDismiss) { Text("Kapat") }
            }
        },
    )
}
