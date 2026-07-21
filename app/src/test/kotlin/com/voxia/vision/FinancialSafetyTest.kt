package com.voxia.vision

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialSafetyTest {

    @Test
    fun buildWarning_warnsForBanknoteLabels() {
        val warning = FinancialSafety.buildWarning(
            VisionAnalysis(labels = listOf(VisionLabel("banknote", 0.92f))),
            language = "fr"
        )

        assertNotNull(warning)
        assertTrue(warning!!.contains("je ne peux pas identifier la valeur", ignoreCase = true))
        assertTrue(warning.contains("authenticité", ignoreCase = true))
    }

    @Test
    fun buildWarning_warnsForCurrencyText() {
        val warning = FinancialSafety.buildWarning(
            VisionAnalysis(text = "Banque des Etats de l'Afrique Centrale BEAC 5000 FCFA"),
            language = "en"
        )

        assertNotNull(warning)
        assertTrue(warning!!.contains("cannot identify the value", ignoreCase = true))
        assertTrue(warning.contains("verify authenticity", ignoreCase = true))
    }

    @Test
    fun buildWarning_doesNotWarnForNonFinancialTicket() {
        val warning = FinancialSafety.buildWarning(
            VisionAnalysis(labels = listOf(VisionLabel("ticket", 0.88f))),
            language = "fr"
        )

        assertNull(warning)
    }
}
