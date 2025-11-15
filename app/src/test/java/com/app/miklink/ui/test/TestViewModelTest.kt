package com.app.miklink.ui.test

import androidx.lifecycle.SavedStateHandle
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.dao.ProbeConfigDao
import com.app.miklink.data.db.dao.ReportDao
import com.app.miklink.data.db.dao.TestProfileDao
import com.app.miklink.data.repository.AppRepository
import com.app.miklink.utils.RateParser
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit Test Suite for TestViewModel.isRateOk()
 *
 * Testa la logica di validazione della velocità del link contro una soglia minima.
 * Il metodo `isRateOk()` utilizza `RateParser.parseToMbps()` per normalizzare
 * entrambi i valori (rate effettivo e soglia) a Mbps e confrontarli.
 *
 * I test mockano `RateParser` per isolare completamente la logica di `isRateOk()`.
 *
 * Casi testati:
 * - Valori null/blank (RateParser ritorna 0)
 * - Valori non riconosciuti "unknown" (RateParser ritorna 0)
 * - Valori validi sopra/sotto/uguali alla soglia
 */
class TestViewModelTest {

    private lateinit var viewModel: TestViewModel

    @Before
    fun setup() {
        // Mock RateParser object per isolare la logica di isRateOk
        mockkObject(RateParser)

        // Mock delle dipendenze Hilt richieste dal costruttore
        // (non necessarie per testare isRateOk, ma obbligatorie per istanziare il ViewModel)
        val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
        val clientDao: ClientDao = mockk(relaxed = true)
        val probeDao: ProbeConfigDao = mockk(relaxed = true)
        val profileDao: TestProfileDao = mockk(relaxed = true)
        val reportDao: ReportDao = mockk(relaxed = true)
        val repository: AppRepository = mockk(relaxed = true)
        val moshi: Moshi = Moshi.Builder().build()

        viewModel = TestViewModel(
            savedStateHandle = savedStateHandle,
            clientDao = clientDao,
            probeDao = probeDao,
            profileDao = profileDao,
            reportDao = reportDao,
            repository = repository,
            moshi = moshi
        )
    }

    @After
    fun tearDown() {
        // Pulisci i mock di RateParser dopo ogni test
        unmockkObject(RateParser)
    }

    // ============================================
    // TEST: Input null o blank
    // ============================================

    @Test
    fun `isRateOk with null rate returns false`() {
        // Mock: RateParser.parseToMbps(null) -> 0
        every { RateParser.parseToMbps(null) } returns 0
        // Mock: RateParser.parseToMbps("100") -> 100 (soglia)
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = null, min = "100")

        assertFalse("Rate nullo (parseToMbps ritorna 0) dovrebbe fallire la validazione", result)
    }

    @Test
    fun `isRateOk with blank rate returns false`() {
        // Mock: RateParser.parseToMbps("  ") -> 0
        every { RateParser.parseToMbps("  ") } returns 0
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "  ", min = "100")

        assertFalse("Rate vuoto (parseToMbps ritorna 0) dovrebbe fallire la validazione", result)
    }

    @Test
    fun `isRateOk with empty rate returns false`() {
        // Mock: RateParser.parseToMbps("") -> 0
        every { RateParser.parseToMbps("") } returns 0
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "", min = "100")

        assertFalse("Rate stringa vuota (parseToMbps ritorna 0) dovrebbe fallire la validazione", result)
    }

    // ============================================
    // TEST: Valori non riconosciuti
    // ============================================

    @Test
    fun `isRateOk with unknown rate returns false`() {
        // CASO 1 RICHIESTO: "unknown" -> parseToMbps ritorna 0 -> isRateOk ritorna false
        every { RateParser.parseToMbps("unknown") } returns 0
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "unknown", min = "100")

        assertFalse("Rate 'unknown' (parseToMbps ritorna 0) dovrebbe fallire la validazione", result)
    }

    @Test
    fun `isRateOk with invalid format returns false`() {
        // Mock: RateParser.parseToMbps("invalid-speed") -> 0
        every { RateParser.parseToMbps("invalid-speed") } returns 0
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "invalid-speed", min = "100")

        assertFalse("Rate con formato non valido (parseToMbps ritorna 0) dovrebbe fallire", result)
    }

    // ============================================
    // TEST: Valori validi - Confronto con soglia
    // ============================================

    @Test
    fun `isRateOk with rate above threshold returns true`() {
        // CASO 3 RICHIESTO: "150Mbps" (soglia 100) -> parseToMbps ritorna 150 -> isRateOk ritorna true
        every { RateParser.parseToMbps("150Mbps") } returns 150
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "150Mbps", min = "100")

        assertTrue("Rate 150 Mbps (mockato) sopra soglia 100 dovrebbe passare", result)
    }

    @Test
    fun `isRateOk with rate below threshold returns false`() {
        // CASO 4 RICHIESTO: "50Mbps" (soglia 100) -> parseToMbps ritorna 50 -> isRateOk ritorna false
        every { RateParser.parseToMbps("50Mbps") } returns 50
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "50Mbps", min = "100")

        assertFalse("Rate 50 Mbps (mockato) sotto soglia 100 dovrebbe fallire", result)
    }

    @Test
    fun `isRateOk with rate equal to threshold returns true`() {
        // CASO 5 RICHIESTO: "100Mbps" (soglia 100) -> parseToMbps ritorna 100 -> isRateOk ritorna true (test del limite)
        every { RateParser.parseToMbps("100Mbps") } returns 100
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "100Mbps", min = "100")

        assertTrue("Rate uguale alla soglia (100 Mbps mockato) dovrebbe passare", result)
    }

    // ============================================
    // TEST: Formati diversi (Gigabit)
    // ============================================

    @Test
    fun `isRateOk with 1Gbps rate and 100 threshold returns true`() {
        // Mock: 1Gbps -> 1000 Mbps
        every { RateParser.parseToMbps("1Gbps") } returns 1000
        every { RateParser.parseToMbps("100") } returns 100

        val result = viewModel.isRateOk(rate = "1Gbps", min = "100")

        assertTrue("1 Gbps (mockato a 1000 Mbps) sopra soglia 100 dovrebbe passare", result)
    }

    @Test
    fun `isRateOk with 1G rate and 1000 threshold returns true`() {
        // Mock: 1G -> 1000 Mbps
        every { RateParser.parseToMbps("1G") } returns 1000
        every { RateParser.parseToMbps("1000") } returns 1000

        val result = viewModel.isRateOk(rate = "1G", min = "1000")

        assertTrue("1G (mockato a 1000 Mbps) uguale a soglia 1000 dovrebbe passare", result)
    }

    @Test
    fun `isRateOk with 1G rate and 1001 threshold returns false`() {
        // Mock: 1G -> 1000 Mbps
        every { RateParser.parseToMbps("1G") } returns 1000
        every { RateParser.parseToMbps("1001") } returns 1001

        val result = viewModel.isRateOk(rate = "1G", min = "1001")

        assertFalse("1G (mockato a 1000 Mbps) sotto soglia 1001 dovrebbe fallire", result)
    }

    // ============================================
    // TEST: Edge Cases - Soglia non standard
    // ============================================

    @Test
    fun `isRateOk with threshold as Gbps format`() {
        // Mock: 1.5Gbps -> 1500 Mbps, 1Gbps -> 1000 Mbps
        every { RateParser.parseToMbps("1.5Gbps") } returns 1500
        every { RateParser.parseToMbps("1Gbps") } returns 1000

        val result = viewModel.isRateOk(rate = "1.5Gbps", min = "1Gbps")

        assertTrue("Rate 1.5 Gbps (mockato a 1500) sopra soglia 1 Gbps (mockato a 1000) dovrebbe passare", result)
    }

    @Test
    fun `isRateOk with both values as numeric Mbps`() {
        // Mock: valori numerici puri
        every { RateParser.parseToMbps("500") } returns 500
        every { RateParser.parseToMbps("250") } returns 250

        val result = viewModel.isRateOk(rate = "500", min = "250")

        assertTrue("Rate 500 Mbps (mockato) sopra soglia 250 Mbps (mockato) dovrebbe passare", result)
    }

    @Test
    fun `isRateOk with zero threshold always passes for non-zero rate`() {
        // Mock: 10Mbps -> 10, soglia 0 -> 0
        every { RateParser.parseToMbps("10Mbps") } returns 10
        every { RateParser.parseToMbps("0") } returns 0

        val result = viewModel.isRateOk(rate = "10Mbps", min = "0")

        assertTrue("Qualsiasi rate positivo (mockato a 10) dovrebbe superare soglia 0", result)
    }

    @Test
    fun `isRateOk with zero rate and zero threshold returns true`() {
        // Mock: 0 -> 0 per entrambi
        every { RateParser.parseToMbps("0") } returns 0

        val result = viewModel.isRateOk(rate = "0", min = "0")

        assertTrue("Rate 0 (mockato) uguale a soglia 0 (mockato) dovrebbe passare", result)
    }

    // ============================================
    // TEST: Casi realistici da MikroTik
    // ============================================

    @Test
    fun `isRateOk with 10G interface and 1000 threshold returns true`() {
        // Mock: 10G -> 10000 Mbps
        every { RateParser.parseToMbps("10G") } returns 10000
        every { RateParser.parseToMbps("1000") } returns 1000

        val result = viewModel.isRateOk(rate = "10G", min = "1000")

        assertTrue("Interfaccia 10G (mockato a 10000 Mbps) dovrebbe superare soglia 1000", result)
    }

    @Test
    fun `isRateOk with 100M interface and 1000 threshold returns false`() {
        // Mock: 100M -> 100 Mbps
        every { RateParser.parseToMbps("100M") } returns 100
        every { RateParser.parseToMbps("1000") } returns 1000

        val result = viewModel.isRateOk(rate = "100M", min = "1000")

        assertFalse("Interfaccia 100M (mockato a 100 Mbps) dovrebbe fallire soglia 1000", result)
    }
}

