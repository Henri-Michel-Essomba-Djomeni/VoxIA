package com.voxia.vision

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductCatalogTest {

    private val header = listOf(
        "barcode",
        "brand",
        "name_fr",
        "name_en",
        "category_fr",
        "category_en",
        "source",
        "source_date",
        "warning_fr",
        "warning_en"
    ).joinToString("\t")

    @Test
    fun fromTsv_loadsOnlySourcedProducts() {
        val validRow = listOf(
            "3760000000012",
            "Marque Test",
            "Biscuit test",
            "Test biscuit",
            "alimentation",
            "food",
            "catalogue pilote",
            "2026-07-21",
            "Vérifier l'étiquette avant usage.",
            "Check the label before use."
        ).joinToString("\t")
        val missingSourceRow = listOf(
            "3760000000099",
            "Sans Source",
            "Produit incomplet",
            "",
            "",
            "",
            "",
            "",
            "",
            ""
        ).joinToString("\t")

        val catalog = ProductCatalog.fromTsv("# commentaire\n$header\n$validRow\n$missingSourceRow")

        assertEquals(1, catalog.size())
        assertEquals("Biscuit test", catalog.find("3760000000012")?.nameFr)
        assertNull(catalog.find("3760000000099"))
    }

    @Test
    fun find_normalizesDecoratedBarcode() {
        val row = listOf(
            "3760000000012",
            "Marque Test",
            "Biscuit test",
            "Test biscuit",
            "alimentation",
            "food",
            "catalogue pilote",
            "2026-07-21",
            "",
            ""
        ).joinToString("\t")
        val catalog = ProductCatalog.fromTsv("$header\n$row")

        assertEquals("Biscuit test", catalog.find("GTIN 3760000000012")?.nameFr)
    }

    @Test
    fun productVoiceFormatter_reportsUnknownProductWithoutInventingDetails() {
        val result = VisionAnalysis(
            labels = listOf(VisionLabel("food", 0.91f)),
            barcodes = listOf("3760000000012"),
            text = "Marque visible"
        )

        val message = ProductVoiceFormatter.build(result, ProductCatalog.empty(), "fr")

        assertTrue(message?.contains("Produit inconnu dans le catalogue local") == true)
        assertTrue(message?.contains("Je ne fournis pas de prix") == true)
        assertTrue(message?.contains("Catégorie probable") == true)
    }

    @Test
    fun productVoiceFormatter_usesSourceAndDateForKnownProduct() {
        val row = listOf(
            "3760000000012",
            "Marque Test",
            "Biscuit test",
            "Test biscuit",
            "alimentation",
            "food",
            "catalogue pilote",
            "2026-07-21",
            "",
            ""
        ).joinToString("\t")
        val catalog = ProductCatalog.fromTsv("$header\n$row")

        val message = ProductVoiceFormatter.build(
            VisionAnalysis(barcodes = listOf("3760000000012")),
            catalog,
            "fr"
        )

        assertTrue(message?.contains("Marque Test Biscuit test") == true)
        assertTrue(message?.contains("catalogue pilote") == true)
        assertTrue(message?.contains("2026-07-21") == true)
    }
}
