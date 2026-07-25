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

@HiltViewModel
class BrandSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val deviceType = runCatching {
        DeviceType.valueOf(savedStateHandle.get<String>("deviceType") ?: "TV")
    }.getOrDefault(DeviceType.TV)

    /** Klima için ayrı, gerçek IR veritabanı henüz olmadığından tek bir jenerik profil sunulur. */
    val isAcDeviceType: Boolean = deviceType == DeviceType.AC

    private val allBrands: List<BrandIrCodeSet> =
        if (isAcDeviceType) listOf(BrandIrDatabase.GENERIC_AC_PLACEHOLDER)
        else BrandIrDatabase.brands.sortedBy { it.displayNameEn }

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
