package com.atakolstudio.sure.ui.screens.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.atakolstudio.sure.data.ir.BrandIrCodeSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandSelectionScreen(
    onBrandSelected: (BrandIrCodeSet) -> Unit,
    onManualSearchClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrandSelectionViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val brands by viewModel.filteredBrands.collectAsState()

    val title = when (viewModel.screenMode) {
        BrandScreenMode.AC_GENERIC_ONLY -> "Klima Profili"
        BrandScreenMode.NO_DATABASE_YET -> "Cihazınızı Bulun"
        BrandScreenMode.FULL_BRAND_LIST -> "Markanızı Bulun"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        when (viewModel.screenMode) {
            BrandScreenMode.NO_DATABASE_YET -> {
                NoDatabaseYetContent(modifier = Modifier.padding(padding), onManualSearchClick = onManualSearchClick)
            }
            BrandScreenMode.AC_GENERIC_ONLY -> {
                Column(Modifier.padding(padding).fillMaxSize()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        Text(
                            "Klima kumandaları, TV'lerin aksine her tuşta tüm durumu (sıcaklık+mod+fan) " +
                                "tek seferde gönderir. Şu an için tek bir jenerik/örnek profil sunuluyor; " +
                                "marka-özel klima desteği ileride eklenecek.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    BrandList(brands = brands, onBrandSelected = onBrandSelected)
                }
            }
            BrandScreenMode.FULL_BRAND_LIST -> {
                Column(Modifier.padding(padding).fillMaxSize()) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        placeholder = { Text("Marka ara...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Surface(
                        onClick = onManualSearchClick,
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Markamı Bilmiyorum", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Kod tarayarak veya elle IR kodu girerek bulun",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    BrandList(brands = brands, onBrandSelected = onBrandSelected)
                }
            }
        }
    }
}

@Composable
private fun BrandList(brands: List<BrandIrCodeSet>, onBrandSelected: (BrandIrCodeSet) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(brands, key = { it.brandKey }) { brand ->
            BrandRow(brand = brand, onClick = { onBrandSelected(brand) })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

/**
 * AV Alıcısı, Ortam Yayıncısı, Disk Oynatıcı, Projektör, Ev Otomasyonu gibi
 * kategoriler için henüz özel bir marka veritabanımız yok. Bu kategorilerde
 * (yanıltıcı biçimde) TV marka listesini göstermek yerine, kullanıcıyı doğrudan
 * gerçek bir çözüme — Kod Tarama / Elle Kod Gir — yönlendiriyoruz.
 */
@Composable
private fun NoDatabaseYetContent(modifier: Modifier = Modifier, onManualSearchClick: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Bu cihaz türü için marka listesi yok",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Elimizdeki gerçek IR kod veritabanı şu an için yalnızca televizyonlara özeldir. " +
                "Bu kategoride markanızı listeleyemiyoruz — ama cihazınızı yine de bulabilirsiniz:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onManualSearchClick,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Kod Tarama / Elle Kod Gir", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "\"Kör Tarama\" modu, gerçek kumandalardan derlenmiş 371 koddan " +
                "oluştuğu için TV dışındaki cihazlarda da (eski VCR/disk oynatıcı " +
                "modülleri dahil) işe yarayabilir.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BrandRow(brand: BrandIrCodeSet, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Tv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(brand.displayNameEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    brand.displayNameLocal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (!brand.verified) {
                AssistChip(onClick = {}, label = { Text("Doğrulanmamış", style = MaterialTheme.typography.labelMedium) })
            }
        }
    }
}
