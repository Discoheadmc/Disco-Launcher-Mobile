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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.discocock.launcher.DiscoApplication
import com.discocock.launcher.ui.theme.AccentChoices
import com.discocock.launcher.ui.theme.LocalLaunchButtonColor
import kotlinx.coroutines.launch

/** Appearance + integration settings, mirroring the desktop Prism-minimal theme. */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val container = (context.applicationContext as DiscoApplication).container
    val scope = rememberCoroutineScope()

    val accent by container.settings.accentColor.collectAsState(initial = "indigo")
    val launchButtonHex by container.settings.launchButtonColor.collectAsState(initial = "")
    val clientId by container.settings.microsoftClientId.collectAsState(initial = "")

    var launchButtonInput by remember { mutableStateOf(launchButtonHex) }
    var clientInput by remember { mutableStateOf(clientId) }

    LaunchedEffect(launchButtonHex) { launchButtonInput = launchButtonHex }
    LaunchedEffect(clientId) { clientInput = clientId }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Accent color — the 8 Prism-minimal swatches.
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

        // Custom launch-button color (mode-independent, like the desktop).
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { scope.launch { container.settings.setLaunchButtonColor(launchButtonInput.trim()) } },
                colors = if (LocalLaunchButtonColor.current == Color.Unspecified) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(containerColor = LocalLaunchButtonColor.current)
                },
            ) { Text("Kaydet") }
            TextButtonClear {
                scope.launch { container.settings.setLaunchButtonColor("") }
            }
        }
        Spacer(Modifier.height(20.dp))

        // Microsoft client id.
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

@Composable
private fun TextButtonClear(onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) { Text("Sıfırla") }
}
