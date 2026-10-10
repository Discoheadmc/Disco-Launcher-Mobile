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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.data.Instance
import com.discocock.launcher.game.GameInstaller
import com.discocock.launcher.game.JvmCommandBuilder
import com.discocock.launcher.game.RuntimeManager
import com.discocock.launcher.net.ManifestVersion
import com.discocock.launcher.ui.theme.LocalLaunchButtonColor
import kotlinx.coroutines.flow.first
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
    val runtimeProgress by container.runtimes.progress.collectAsState()
    val activeDownloads by container.downloads.active.collectAsState()
    val launchState by container.launcher.state.collectAsState()
    val launchButtonColor = LocalLaunchButtonColor.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var detailInstance by remember { mutableStateOf<Instance?>(null) }
    var launchError by remember { mutableStateOf<String?>(null) }
    var showLog by remember { mutableStateOf(false) }
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

            // Arka planda süren indirmeler — her yerden durdurulabilir.
            if (activeDownloads.isNotEmpty()) {
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Arka plan indirmeleri (${activeDownloads.size})",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { container.downloads.cancelAll() }) {
                                Text("Tümünü durdur")
                            }
                        }
                        activeDownloads.forEach { dl ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    dl.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { container.downloads.cancel(dl.id) }) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Durdur",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

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
                        "Sürüm — ${progress.stage} — ${progress.current}/${progress.total}",
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

            val rp = runtimeProgress
            when (rp) {
                is RuntimeManager.RuntimeProgress.Working -> {
                    LinearProgressIndicator(
                        progress = {
                            if (rp.total <= 0) 0f else rp.downloaded.toFloat() / rp.total.toFloat()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Java runtime — ${rp.stage} — ${rp.downloaded / (1024 * 1024)}/${rp.total / (1024 * 1024)} MB",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                is RuntimeManager.RuntimeProgress.Failed -> {
                    Text(
                        "Runtime hatası: ${rp.message}",
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
                    // Yeni instance: varsayılan RAM ayarını al
                    val memory = container.settings.defaultMemoryMb.first()
                    val created = container.instances.create(name, versionId, memory)
                    if (created.isSuccess) {
                        showCreateDialog = false
                        // Zorunlu kurulum — arka planda çalışır, ekran değişse de sürer
                        container.downloads.installVersion(versionId)
                        container.downloads.ensureRuntime()
                    }
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
                container.downloads.installVersion(instance.versionId)
            },
            onSave = { ram, server ->
                scope.launch {
                    container.instances.update(
                        instance.copy(javaMemoryMb = ram, serverAddress = server.trim()),
                    )
                    detailInstance = null
                }
            },
            onShowLog = { showLog = true },
            onDelete = {
                detailInstance = null
                scope.launch { container.instances.delete(instance.id) }
            },
        )
    }

    launchState.let { state ->
        when (state) {
            is com.discocock.launcher.game.GameLauncher.LaunchState.Failed -> {
                AlertDialog(
                    onDismissRequest = { container.launcher.acknowledge() },
                    title = { Text("Başlatılamadı") },
                    text = { Text(state.message) },
                    confirmButton = {
                        TextButton(onClick = { container.launcher.acknowledge() }) { Text("Tamam") }
                    },
                )
            }
            is com.discocock.launcher.game.GameLauncher.LaunchState.Exited -> {
                AlertDialog(
                    onDismissRequest = { container.launcher.acknowledge() },
                    title = { Text("Oyun çıktı — kod ${state.code}") },
                    text = {
                        Column {
                            Text(
                                if (state.code == 0) "Normal çıkış." else "JVM çalıştı ve sonlandı.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                state.log.takeLast(24).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 24,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { container.launcher.acknowledge() }) { Text("Tamam") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showLog = true
                            container.launcher.acknowledge()
                        }) { Text("Tüm log") }
                    },
                )
            }
            else -> Unit
        }
    }

    if (showLog) {
        LogDialog(lines = container.launcher.logSnapshot(), onDismiss = { showLog = false })
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
    val runtime = container.runtimes.activeRuntime(container.settings.preferredRuntime.first())
    if (runtime == null) {
        val downloading = container.downloads.active.value.isNotEmpty()
        if (!downloading) {
            // JRE otomatik kurulumunu arka planda başlat
            container.downloads.ensureRuntime(container.settings.preferredRuntime.first())
        }
        onError(
            "Java runtime hazır değil — cihazına uygun JRE otomatik indiriliyor. " +
                "İndirme bitince tekrar Başlat.",
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
    val extraArgs = container.settings.extraJvmArgs.first()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    val spec = JvmCommandBuilder.build(
        javaBin = javaBin,
        instance = instance,
        info = info,
        versionsDir = container.paths.versionsDir,
        assetsDir = container.paths.assetsDir,
        librariesDir = container.paths.librariesDir,
        instanceDir = container.instances.instanceDir(instance),
        account = account,
        extraJvmArgs = extraArgs,
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
                if (selectedVersion != null && requiresJava21(selectedVersion)) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "⚠ Bu sürüm Java 21 gerektirir; katalogda henüz yok. " +
                            "JRE 17 ile çalışan 1.20.4 ve altını öneriyoruz.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
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

/** 1.20.5+ needs Java 21, which is not in the runtime catalog yet. */
private fun requiresJava21(versionId: String?): Boolean {
    if (versionId == null) return false
    val match = Regex("""^1\.(\d+)(?:\.(\d+))?""").find(versionId) ?: return false
    val minor = match.groupValues[1].toIntOrNull() ?: return false
    val patch = match.groupValues[2].toIntOrNull() ?: 0
    return minor > 20 || (minor == 20 && patch >= 5)
}

@Composable
private fun LogDialog(lines: List<String>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Oyun günlüğü (${lines.size} satır)") },
        text = {
            LazyColumn(modifier = Modifier.height(320.dp)) {
                items(lines) { line ->
                    Text(
                        line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
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
    onSave: (ramMb: Int, serverAddress: String) -> Unit,
    onShowLog: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val runtimes by container.runtimes.runtimes.collectAsState()

    var editRam by remember(instance.id) { mutableStateOf(instance.javaMemoryMb.toFloat()) }
    var editServer by remember(instance.id) { mutableStateOf(instance.serverAddress) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(instance.name) },
        text = {
            Column {
                Text("Sürüm: ${instance.versionId}")
                Text(
                    if (installed) "Durum: kurulu" else "Durum: kurulmadı",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))

                Text("Java RAM", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${editRam.toInt()} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = editRam,
                    onValueChange = { editRam = it },
                    valueRange = 512f..8192f,
                    steps = 29,
                )

                OutlinedTextField(
                    value = editServer,
                    onValueChange = { editServer = it },
                    label = { Text("Sunucu adresi (opsiyonel)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (runtimes.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Java runtime kurulu değil — instance oluşturma sırasında otomatik indirilir.",
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
                TextButton(onClick = { onSave(editRam.toInt(), editServer) }) { Text("Kaydet") }
                if (!installed) {
                    TextButton(onClick = onInstall) {
                        Icon(Icons.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kur")
                    }
                }
                TextButton(onClick = onShowLog) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Log")
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
