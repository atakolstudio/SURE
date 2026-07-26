package com.atakolstudio.sure.ui.screens.manualsearch

import androidx.lifecycle.SavedStateHandle
import com.atakolstudio.sure.data.ir.BrandIrCodeSet
import com.atakolstudio.sure.data.ir.BrandIrDatabase
import com.atakolstudio.sure.data.ir.CUSTOM_BRAND_KEY
import com.atakolstudio.sure.data.ir.IrProtocol
import com.atakolstudio.sure.data.ir.IrTransmitResult
import com.atakolstudio.sure.data.ir.IrTransmitter
import com.atakolstudio.sure.data.ir.LircBlindScanLoader
import com.atakolstudio.sure.data.ir.RemoteButton
import com.atakolstudio.sure.testutil.FakeDeviceRepository
import com.atakolstudio.sure.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.any
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ManualSearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var irTransmitter: IrTransmitter
    private lateinit var repository: FakeDeviceRepository
    private lateinit var lircLoader: LircBlindScanLoader

    private val fakeLircCandidate = BrandIrCodeSet(
        brandKey = "lirc_blind_0",
        displayNameEn = "Bilinmeyen Cihaz",
        displayNameLocal = "LIRC veritabanı · NEC",
        protocol = IrProtocol.NEC,
        verified = false,
        address = 0x55,
        commands = mapOf(RemoteButton.POWER to 0x11, RemoteButton.VOLUME_UP to 0x12)
    )

    private fun createViewModel(): ManualSearchViewModel {
        val savedStateHandle = SavedStateHandle(
            mapOf("deviceType" to "TV", "connectionType" to "TRADITIONAL_IR")
        )
        return ManualSearchViewModel(irTransmitter, repository, lircLoader, savedStateHandle)
    }

    @Before
    fun setUp() {
        irTransmitter = mockk(relaxed = true)
        every { irTransmitter.send(any(), any()) } returns IrTransmitResult.Success
        repository = FakeDeviceRepository()
        lircLoader = mockk()
        every { lircLoader.loadCandidates() } returns listOf(fakeLircCandidate)
    }

    @Test
    fun `baslangicta yalnizca bilinen markalar aday olarak yuklenir`() {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.candidates).isEqualTo(BrandIrDatabase.brands)
        assertThat(viewModel.uiState.value.scanTier).isEqualTo(ScanTier.STANDARD)
    }

    @Test
    fun `BLIND tarama modu secilince LIRC adaylari eklenir`() {
        val viewModel = createViewModel()
        viewModel.setScanTier(ScanTier.BLIND)

        val candidates = viewModel.uiState.value.candidates
        assertThat(candidates).contains(fakeLircCandidate)
        assertThat(candidates.size).isEqualTo(BrandIrDatabase.brands.size + 1)
    }

    @Test
    fun `STANDARD moda donulunce LIRC adaylari kaldirilir`() {
        val viewModel = createViewModel()
        viewModel.setScanTier(ScanTier.BLIND)
        viewModel.setScanTier(ScanTier.STANDARD)

        assertThat(viewModel.uiState.value.candidates).isEqualTo(BrandIrDatabase.brands)
    }

    @Test
    fun `nextCandidate index'i bir arttirir`() {
        val viewModel = createViewModel()
        val initialIndex = viewModel.uiState.value.currentIndex

        viewModel.nextCandidate()

        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(initialIndex + 1)
        assertThat(viewModel.uiState.value.exhausted).isFalse()
    }

    @Test
    fun `son adaydan sonra nextCandidate exhausted=true yapar`() {
        val viewModel = createViewModel()
        val totalCandidates = viewModel.uiState.value.candidates.size

        repeat(totalCandidates) { viewModel.nextCandidate() }

        assertThat(viewModel.uiState.value.exhausted).isTrue()
    }

    @Test
    fun `previousCandidate index'i bir azaltir`() {
        val viewModel = createViewModel()
        viewModel.nextCandidate()
        viewModel.nextCandidate()

        viewModel.previousCandidate()

        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(1)
    }

    @Test
    fun `previousCandidate index 0'dayken hicbir sey yapmaz`() {
        val viewModel = createViewModel()
        viewModel.previousCandidate()
        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(0)
    }

    @Test
    fun `restartScan index'i sifirlar ve exhausted'i temizler`() {
        val viewModel = createViewModel()
        val totalCandidates = viewModel.uiState.value.candidates.size
        repeat(totalCandidates) { viewModel.nextCandidate() }
        assertThat(viewModel.uiState.value.exhausted).isTrue()

        viewModel.restartScan()

        assertThat(viewModel.uiState.value.currentIndex).isEqualTo(0)
        assertThat(viewModel.uiState.value.exhausted).isFalse()
    }

    @Test
    fun `testCurrentCandidate IrTransmitter uzerinden guc tusunu gonderir`() {
        val viewModel = createViewModel()
        viewModel.testCurrentCandidate()

        io.mockk.verify { irTransmitter.send(BrandIrDatabase.brands.first(), RemoteButton.POWER) }
    }

    @Test
    fun `bilinen marka onaylandiginda repository'e brandKey ile kaydedilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val expectedBrand = viewModel.uiState.value.candidates[0]

        viewModel.confirmCurrentMatch("Salon TV")
        advanceUntilIdle()

        assertThat(repository.currentDevices).hasSize(1)
        val saved = repository.currentDevices.first()
        assertThat(saved.brandKey).isEqualTo(expectedBrand.brandKey)
        assertThat(saved.nickname).isEqualTo("Salon TV")
        assertThat(viewModel.uiState.value.savedDeviceId).isEqualTo(saved.id)
    }

    @Test
    fun `kor taramadan bulunan cihaz custom olarak kaydedilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        viewModel.setScanTier(ScanTier.BLIND)
        // fakeLircCandidate'a ulasana kadar ilerle
        val indexOfLirc = viewModel.uiState.value.candidates.indexOf(fakeLircCandidate)
        repeat(indexOfLirc) { viewModel.nextCandidate() }

        viewModel.confirmCurrentMatch("")
        advanceUntilIdle()

        assertThat(repository.currentDevices).hasSize(1)
        val saved = repository.currentDevices.first()
        assertThat(saved.brandKey).isEqualTo(CUSTOM_BRAND_KEY)
        assertThat(saved.customProtocol).isEqualTo(IrProtocol.NEC.name)
        assertThat(saved.customAddress).isEqualTo(0x55)
    }

    @Test
    fun `bos isim verilirse marka adi varsayilan olarak kullanilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel()
        val expectedBrand = viewModel.uiState.value.candidates[0]

        viewModel.confirmCurrentMatch("")
        advanceUntilIdle()

        assertThat(repository.currentDevices.first().nickname).isEqualTo(expectedBrand.displayNameEn)
    }

    @Test
    fun `toggleAutoScan basladiginda isAutoScanning true olur`() {
        val viewModel = createViewModel()
        viewModel.toggleAutoScan()
        assertThat(viewModel.uiState.value.isAutoScanning).isTrue()
    }

    @Test
    fun `stopAutoScan cagrilinca isAutoScanning false olur`() {
        val viewModel = createViewModel()
        viewModel.toggleAutoScan()
        viewModel.stopAutoScan()
        assertThat(viewModel.uiState.value.isAutoScanning).isFalse()
    }
}
