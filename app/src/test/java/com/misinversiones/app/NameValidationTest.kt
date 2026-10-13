package com.misinversiones.app

import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioPayload
import com.misinversiones.app.data.util.NameUtils
import com.misinversiones.app.data.util.SectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NameValidationTest {

    @Test
    fun testNameNormalization() {
        val raw1 = "   Fondo  Indexado   S&P  500  "
        val normalized1 = NameUtils.normalizeName(raw1)
        assertEquals("fondo indexado s&p 500", normalized1)

        val raw2 = "ACCIONES  TESLA "
        val normalized2 = NameUtils.normalizeName(raw2)
        assertEquals("acciones tesla", normalized2)
    }

    @Test
    fun testDuplicateDetectionAcrossSections() {
        val investments = listOf(
            Investment(name = "Fondo Vanguard", category = "Fondos idx")
        )
        val reserved = listOf(
            Investment(name = "Cuenta Sabadell", category = "C. Remunerada")
        )

        // Exact match
        assertTrue(NameUtils.isDuplicateName("Fondo Vanguard", investments, reserved))

        // Normalized space match
        assertTrue(NameUtils.isDuplicateName("  fondo   vanguard ", investments, reserved))

        // Cross-section match with reserved
        assertTrue(NameUtils.isDuplicateName("CUENTA   sabadell", investments, reserved))

        // Non-duplicate name
        assertFalse(NameUtils.isDuplicateName("Nuevo Depósito BBVA", investments, reserved))
    }

    @Test
    fun testSelfExclusionWhenEditing() {
        val investments = listOf(
            Investment(name = "Fondo Vanguard", category = "Fondos idx"),
            Investment(name = "Acciones Apple", category = "Acciones")
        )
        val reserved = emptyList<Investment>()

        // Editing item at index 0 and keeping name "Fondo Vanguard" should be allowed (self excluded)
        assertFalse(
            NameUtils.isDuplicateName(
                name = "FONDO   vanguard",
                investments = investments,
                reservedInvestments = reserved,
                excludeSection = SectionType.PORTFOLIO,
                excludeIndex = 0
            )
        )

        // Editing item at index 0 and changing name to "Acciones Apple" should fail (matches item at index 1)
        assertTrue(
            NameUtils.isDuplicateName(
                name = "acciones  apple",
                investments = investments,
                reservedInvestments = reserved,
                excludeSection = SectionType.PORTFOLIO,
                excludeIndex = 0
            )
        )
    }

    @Test
    fun testValidatePayloadNames() {
        val duplicatePayload = PortfolioPayload(
            investments = listOf(
                Investment(name = "ETF World", category = "ETF")
            ),
            reservedInvestments = listOf(
                Investment(name = " etf   world ", category = "ETF")
            )
        )

        val result = NameUtils.validatePayloadNames(duplicatePayload)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("duplicado") == true)
    }
}
