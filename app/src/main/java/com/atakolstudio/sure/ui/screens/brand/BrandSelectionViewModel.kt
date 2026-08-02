package com.atakolstudio.sure.ui.screens.brand

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.atakolstudio.sure.data.ir.BrandIrCodeSet
import com.atakolstudio.sure.data.ir.BrandIrDatabase
import com.atakolstudio.sure.domain.model.DeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Marka Seçimi ekranının hangi modda çalışacağını belirler. Gerçek IR kod
 * veritabanımız (BrandIrDatabase) yalnızca TV'ler için kapsamlıdır; bu yüzden
 * her cihaz türü için AYNI TV marka listesini göstermek yanıltıcıdır (kullanıcı
 * "Disk Oynatıcı" seçse de "Samsung TV" kodlarını görüyor olurdu).
 */
enum class BrandScreenMode {
    /** TV ve Set Üstü Kutu: kapsamlı, gerçek marka veritabanı gösterilir. */
    FULL_BRAND_LIST,
    /** Klima: ayrı bir kod modeli (tam durum) kullandığından tek bir jenerik profil sunulur. */
    AC_GENERIC_ONLY,
    /** AV Alıcısı, Ortam Yayıncısı, Disk Oynatıcı, Projektör, Ev Otomasyonu: bu
     *  kategoriler için henüz özel bir marka veritabanı yok. Yanıltıcı bir TV
     *  listesi göstermek yerine, kullanıcı doğrudan Kod Tarama / Elle Kod Gir'e
     *  yönlendirilir. */
    NO_DATABASE_YET
}

@HiltViewModel
class BrandSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceType = runCatching {
        DeviceType.valueOf(savedStateHandle.get<String>("deviceType") ?: "TV")
    }.getOrDefault(DeviceType.TV)

    val screenMode: BrandScreenMode = when (deviceType) {
        DeviceType.TV, DeviceType.SET_TOP_BOX -> BrandScreenMode.FULL_BRAND_LIST
        DeviceType.AC -> BrandScreenMode.AC_GENERIC_ONLY
        else -> BrandScreenMode.NO_DATABASE_YET
    }

    /** Geriye dönük uyumluluk için: sadece AC modunu ayırt etmek isteyen çağıranlar için. */
    val isAcDeviceType: Boolean get() = screenMode == BrandScreenMode.AC_GENERIC_ONLY

    private val allBrands: List<BrandIrCodeSet> = when (screenMode) {
        BrandScreenMode.AC_GENERIC_ONLY -> listOf(BrandIrDatabase.GENERIC_AC_PLACEHOLDER)
        BrandScreenMode.FULL_BRAND_LIST -> BrandIrDatabase.brands.sortedBy { it.displayNameEn }
        BrandScreenMode.NO_DATABASE_YET -> emptyList()
    }

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filteredBrands = MutableStateFlow(allBrands)
    val filteredBrands: StateFlow<List<BrandIrCodeSet>> = _filteredBrands.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        _filteredBrands.value = if (newQuery.isBlank()) {
            allBrands
        } else {
            allBrands.filter {
                it.displayNameEn.contains(newQuery, ignoreCase = true) ||
                    it.displayNameLocal.contains(newQuery, ignoreCase = true) ||
                    it.brandKey.contains(newQuery, ignoreCase = true)
            }
        }
    }
}
