package com.atakolstudio.sure.ui.screens.devicetype

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.atakolstudio.sure.domain.model.DeviceType

private fun iconFor(type: DeviceType): ImageVector = when (type) {
    DeviceType.TV -> Icons.Filled.Tv
    DeviceType.SET_TOP_BOX -> Icons.Filled.SettingsInputHdmi
    DeviceType.AC -> Icons.Filled.AcUnit
    DeviceType.AV_RECEIVER -> Icons.Filled.Speaker
    DeviceType.STREAMING_MEDIA -> Icons.Filled.Cast
    DeviceType.HOME_AUTOMATION -> Icons.Filled.Home
    DeviceType.DISC_PLAYER -> Icons.Filled.Album
    DeviceType.PROJECTOR -> Icons.Filled.Videocam
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTypeSelectionScreen(
    onTypeSelected: (DeviceType) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cihaz Türünü Seçin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(DeviceType.values().toList()) { type ->
                DeviceTypeCard(type = type, onClick = { onTypeSelected(type) })
            }
        }
    }
}

@Composable
private fun DeviceTypeCard(type: DeviceType, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().height(120.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = iconFor(type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(type.displayNameTr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
