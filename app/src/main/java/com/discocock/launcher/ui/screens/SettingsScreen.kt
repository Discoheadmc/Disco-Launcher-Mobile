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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.ui.theme.AccentChoices
import com.discocock.launcher.ui.theme.LocalLaunchButtonColor
import kotlinx.coroutines.launch

/** Launcher + appearance settings, mirroring the desktop Prism-minimal theme. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val accent by container.settings.accentColor.collectAsState(initial = "indigo")
    val launchButtonHex by container.settings.launchButtonColor.collectAsState(initial = "")
    val clientId by container.settings.microsoftClientId.collectAsState(initial = "")
    val defaultMemory by container.settings.defaultMemoryMb.collectAsState(initial = 2048)
    val jvmArgs by container.settings.extraJvmArgs.collectAsState(initial = "")
    val preferredRuntime by container.settings.preferredRuntime.collectAsState(initial = "")
    val runtimes by container.runtimes.runtimes.collectAsState()

    var launchButtonInput by remember { mutableStateOf(launchButtonHex) }
    var clientInput by remember { mutableStateOf(clientId) }
    var jvmArgsInput by remember { mutableStateOf(jvmArgs) }
    var memoryInput by remember { mutableStateOf(defaultMemory.toFloat()) }

    LaunchedEffect(launchButtonHex) { launchButtonInput = launchButtonHex }
    LaunchedEffect(clientId) { clientInput = clientId }
    LaunchedEffect(jvmArgs) { jvmArgsInput = jvmArgs }
    LaunchedEffect(defaultMemory) { memoryInput = defaultMemory.toFloat() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)

        // ---------------- Launcher ayarları ----------------
        Spacer(Modifier.height(16.dp))
        Text("Launcher Ayarları", style = MaterialTheme.typography.titleMedium)

        Text(
            "Varsayılan Java RAM (yeni instance'lar)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${memoryInput.toInt()} MB",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Slider(
            value = memoryInput,
            onValueChange = { memoryInput = it },
            onValueChangeFinished = {
                scope.launch { container.settings.setDefaultMemoryMb(memoryInput.toInt()) }
            },
            valueRange = 512f..8192f,
            steps = 29,
        )

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = jvmArgsInput,
            onValueChange = { jvmArgsInput = it },
            label = { Text("Ek JVM argümanları") },
            placeholder = { Text("-XX:+UseG1GC -Dfml.earlyprogresswindow=false") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = { scope.launch { container.settings.setExtraJvmArgs(jvmArgsInput) } }) {
            Text("Kaydet")
        }

        Spacer(Modifier.height(16.dp))
        Text("Java Runtime", style = MaterialTheme.typography.titleSmall)
        if (runtimes.isEmpty()) {
            Text(
                "Kurulu runtime yok — bir instance oluşturunca cihazına uygun JRE otomatik indirilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { scope.launch { container.settings.setPreferredRuntime("") } },
        ) {
            RadioButton(
                selected = preferredRuntime.isBlank(),
                onClick = { scope.launch { container.settings.setPreferredRuntime("") } },
            )
            Column {
                Text("Otomatik", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "İlk kurulu runtime kullanılır",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        runtimes.forEach { runtime ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { scope.launch { container.settings.setPreferredRuntime(runtime.name) } },
            ) {
                RadioButton(
                    selected = preferredRuntime == runtime.name,
                    onClick = { scope.launch { container.settings.setPreferredRuntime(runtime.name) } },
                )
                Column {
                    Text(runtime.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        runtime.version,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ---------------- Pil / arka plan ----------------
        Text("Ekran kapalıyken indirme", style = MaterialTheme.typography.titleMedium)
        Text(
            "İndirmeler ön plan servisiyle sürer (CPU kilitli, bildirim görünür). " +
                "Uzun süren indirmelerde derin uyku moduna (Doze) takılmamak için " +
                "aşağıdaki düğmeyle uygulamayı pil optimizasyonundan muaf tutabilirsin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            runCatching {
                context.startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        android.net.Uri.parse("package:" + context.packageName),
                    ),
                )
            }
        }) {
            Text("Pil optimizasyonundan muaf tut")
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ---------------- Görünüm ----------------
        Text("Vurgu rengi", style = MaterialTheme.typography.titleMedium)
        Text(
            "Masaüstü Disco Launcher'ın 8 renk seçicisi",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentChoices.take(4).forEach { (key, color) ->
                AccentSwatch(color = color, selected = accent == key) {
                    scope.launch { container.settings.setAccentColor(key) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentChoices.drop(4).forEach { (key, color) ->
                AccentSwatch(color = color, selected = accent == key) {
                    scope.launch { container.settings.setAccentColor(key) }
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        Text("Başlat butonu rengi", style = MaterialTheme.typography.titleMedium)
        Text(
            "#RRGGBB — boş bırakılırsa tema rengi kullanılır",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = launchButtonInput,
            onValueChange = { launchButtonInput = it },
            label = { Text("Örn. #34D399") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { scope.launch { container.settings.setLaunchButtonColor(launchButtonInput.trim()) } },
                colors = if (LocalLaunchButtonColor.current == Color.Unspecified) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(containerColor = LocalLaunchButtonColor.current)
                },
            ) { Text("Kaydet") }
            TextButton(onClick = { scope.launch { container.settings.setLaunchButtonColor("") } }) {
                Text("Sıfırla")
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        // ---------------- Hesap entegrasyonu ----------------
        Text("Microsoft client_id", style = MaterialTheme.typography.titleMedium)
        Text(
            "Xbox Live destekli ücretsiz bir Azure uygulaması açıp client_id'yi buraya yapıştır; " +
                "Hesaplar sekmesinde Microsoft girişi etkinleşir.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = clientInput,
            onValueChange = { clientInput = it },
            label = { Text("client_id (GUID)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { scope.launch { container.settings.setMicrosoftClientId(clientInput) } }) {
            Text("Kaydet")
        }

        Spacer(Modifier.height(24.dp))
        Text("Hakkında", style = MaterialTheme.typography.titleMedium)
        Text(
            "Disco Launcher Mobile 0.1.0 — XMCL tabanlı Disco Launcher'ın Android portu " +
                "(Kotlin + Jetpack Compose). MIT lisanslı, telemetri yok.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun AccentSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F1115)),
            )
        }
    }
}
