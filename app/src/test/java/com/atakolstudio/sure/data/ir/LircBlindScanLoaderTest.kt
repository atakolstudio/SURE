package com.atakolstudio.sure.data.ir

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LircBlindScanLoaderTest {

    private lateinit var loader: LircBlindScanLoader

    @Before
    fun setUp() {
        loader = LircBlindScanLoader(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `lirc_blind_scan json dosyasi basariyla yukleniyor ve bos degil`() {
        val candidates = loader.loadCandidates()
        assertThat(candidates).isNotEmpty()
    }

    @Test
    fun `her adayin en azindan GUC tusu var`() {
        val candidates = loader.loadCandidates()
        candidates.forEach { candidate ->
            assertThat(candidate.commands).containsKey(RemoteButton.POWER)
        }
    }

    @Test
    fun `her adayin brandKey benzersiz`() {
        val candidates = loader.loadCandidates()
        val keys = candidates.map { it.brandKey }
        assertThat(keys).containsNoDuplicates()
    }

    @Test
    fun `sonuc onbelleklenir - ikinci cagri ayni referansi dondurur`() {
        val first = loader.loadCandidates()
        val second = loader.loadCandidates()
        assertThat(first).isSameInstanceAs(second)
    }

    @Test
    fun `desteklenen protokoller gecerli IrProtocol degerleri`() {
        val candidates = loader.loadCandidates()
        val validProtocols = IrProtocol.entries.toSet()
        candidates.forEach { candidate ->
            assertThat(validProtocols).contains(candidate.protocol)
        }
    }
}
