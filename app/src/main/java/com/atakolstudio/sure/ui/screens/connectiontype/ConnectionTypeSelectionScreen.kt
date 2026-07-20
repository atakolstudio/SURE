package com.atakolstudio.sure.ui.screens.connectiontype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atakolstudio.sure.domain.model.ConnectionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionTypeSelectionScreen(
    onTypeSelected: (ConnectionType) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bağlantı Türünü Seçin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionOptionCard(
                title = "Geleneksel Aygıt (IR)",
                subtitle = "Kızılötesi sinyal ile kontrol edilir. Çoğu TV, klima ve set üstü kutu bu yöntemi kullanır.",
                icon = Icons.Filled.SettingsRemote,
                recommended = true,
                onClick = { onTypeSelected(ConnectionType.TRADITIONAL_IR) }
            )
            ConnectionOptionCard(
                title = "Akıllı Cihaz (WiFi)",
                subtitle = "Ağ üzerinden kontrol edilir. Bu sürümde altyapı hazır, protokol entegrasyonu yakında.",
                icon = Icons.Filled.Wifi,
                recommended = false,
                onClick = { onTypeSelected(ConnectionType.SMART_WIFI) }
            )
        }
    }
}

@Composable
private fun ConnectionOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    recommended: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (recommended) {
                        Spacer(Modifier.width(8.dp))
                        AssistChip(onClick = {}, label = { Text("Varsayılan", style = MaterialTheme.typography.labelMedium) })
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}
