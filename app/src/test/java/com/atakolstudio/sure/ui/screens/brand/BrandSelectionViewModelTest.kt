package com.atakolstudio.sure.ui.screens.brand

import androidx.lifecycle.SavedStateHandle
import com.atakolstudio.sure.data.ir.BrandIrDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BrandSelectionViewModelTest {

    @Test
    fun `TV cihaz turunde tum markalar listelenir`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "TV")))

        assertThat(viewModel.isAcDeviceType).isFalse()
        assertThat(viewModel.filteredBrands.value).isEqualTo(BrandIrDatabase.brands.sortedBy { it.displayNameEn })
    }

    @Test
    fun `AC cihaz turunde yalnizca jenerik klima profili listelenir`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "AC")))

        assertThat(viewModel.isAcDeviceType).isTrue()
        assertThat(viewModel.filteredBrands.value).containsExactly(BrandIrDatabase.GENERIC_AC_PLACEHOLDER)
    }

    @Test
    fun `arama sorgusu marka adina gore filtreler`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "TV")))

        viewModel.onQueryChange("samsung")

        val results = viewModel.filteredBrands.value
        assertThat(results).hasSize(1)
        assertThat(results.first().brandKey).isEqualTo("samsung")
    }

    @Test
    fun `bos arama sorgusu tum markalari gosterir`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "TV")))

        viewModel.onQueryChange("sony")
        viewModel.onQueryChange("")

        assertThat(viewModel.filteredBrands.value).hasSize(BrandIrDatabase.brands.size)
    }

    @Test
    fun `eslesmeyen arama sorgusu bos liste dondurur`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "TV")))

        viewModel.onQueryChange("bu_marka_asla_olmayacak_xyz")

        assertThat(viewModel.filteredBrands.value).isEmpty()
    }

    @Test
    fun `deviceType belirtilmezse varsayilan TV kabul edilir`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(emptyMap()))
        assertThat(viewModel.isAcDeviceType).isFalse()
    }

    @Test
    fun `Disk Oynatici icin marka veritabani gosterilmez (NO_DATABASE_YET)`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "DISC_PLAYER")))

        assertThat(viewModel.screenMode).isEqualTo(BrandScreenMode.NO_DATABASE_YET)
        assertThat(viewModel.filteredBrands.value).isEmpty()
    }

    @Test
    fun `AV Alicisi, Ortam Yayincisi, Projektor ve Ev Otomasyonu icin de marka listesi bos`() {
        listOf("AV_RECEIVER", "STREAMING_MEDIA", "PROJECTOR", "HOME_AUTOMATION").forEach { type ->
            val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to type)))
            assertThat(viewModel.screenMode).isEqualTo(BrandScreenMode.NO_DATABASE_YET)
            assertThat(viewModel.filteredBrands.value).isEmpty()
        }
    }

    @Test
    fun `Set Ustu Kutu icin de tam marka listesi gosterilir (TV ile ayni)`() {
        val viewModel = BrandSelectionViewModel(SavedStateHandle(mapOf("deviceType" to "SET_TOP_BOX")))
        assertThat(viewModel.screenMode).isEqualTo(BrandScreenMode.FULL_BRAND_LIST)
        assertThat(viewModel.filteredBrands.value).isEqualTo(BrandIrDatabase.brands.sortedBy { it.displayNameEn })
    }
}
