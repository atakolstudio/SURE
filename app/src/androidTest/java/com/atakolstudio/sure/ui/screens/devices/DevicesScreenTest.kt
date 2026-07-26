package com.atakolstudio.sure.ui.screens.devices

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.testutil.FakeDeviceRepository
import com.atakolstudio.sure.ui.theme.SureTheme
import org.junit.Rule
import org.junit.Test

/**
 * `DevicesScreen`'in kendi varsayılan `hiltViewModel()` parametresi, testte açıkça
 * bir [DevicesViewModel] örneği geçilerek atlanır — böylece Hilt test altyapısı
 * kurmaya gerek kalmadan, gerçek bir cihaz/emülatörde ekranın doğru davrandığı
 * doğrulanabilir.
 */
class DevicesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleDevice(nickname: String) = SavedDeviceEntity(
        nickname = nickname,
        brandKey = "samsung",
        brandDisplayName = "Samsung",
        deviceType = "TV",
        connectionType = "TRADITIONAL_IR",
        createdAtEpochMillis = 0L,
        lastUsedEpochMillis = 0L
    )

    @Test
    fun bosDurumdaBilgilendirmeMetniGorunur() {
        val repository = FakeDeviceRepository()
        val viewModel = DevicesViewModel(repository)

        composeTestRule.setContent {
            SureTheme {
                DevicesScreen(
                    onAddDeviceClick = {},
                    onDeviceClick = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Henüz cihaz eklemediniz").assertExists()
    }

    @Test
    fun yeniCihazEkleButonuTiklaninca_callbackTetiklenir() {
        val repository = FakeDeviceRepository()
        val viewModel = DevicesViewModel(repository)
        var clicked = false

        composeTestRule.setContent {
            SureTheme {
                DevicesScreen(
                    onAddDeviceClick = { clicked = true },
                    onDeviceClick = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Yeni Cihaz Ekle").performClick()
        composeTestRule.waitForIdle()

        assert(clicked) { "onAddDeviceClick callback'i tetiklenmedi" }
    }

    @Test
    fun eklenenCihazListedeGorunur() {
        val repository = FakeDeviceRepository()
        repository.addDevice(sampleDevice("Salon TV"))
        val viewModel = DevicesViewModel(repository)

        composeTestRule.setContent {
            SureTheme {
                DevicesScreen(
                    onAddDeviceClick = {},
                    onDeviceClick = {},
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Salon TV").assertExists()
    }

    @Test
    fun cihazaTiklaninca_dogruIdIleCallbackTetiklenir() {
        val repository = FakeDeviceRepository()
        val id = repository.addDevice(sampleDevice("Salon TV"))
        val viewModel = DevicesViewModel(repository)
        var clickedId: Long? = null

        composeTestRule.setContent {
            SureTheme {
                DevicesScreen(
                    onAddDeviceClick = {},
                    onDeviceClick = { clickedId = it },
                    viewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Salon TV").performClick()
        composeTestRule.waitForIdle()

        assert(clickedId == id) { "Beklenen cihaz id'si $id, gelen: $clickedId" }
    }
}
