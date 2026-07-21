package com.voxia.vision

import android.content.Context
import com.voxia.utils.PrivacyLog

data class ProductInfo(
    val barcode: String,
    val nameFr: String,
    val nameEn: String,
    val brand: String,
    val categoryFr: String,
    val categoryEn: String,
    val source: String,
    val sourceDate: String,
    val warningFr: String = "",
    val warningEn: String = ""
) {
    fun label(language: String): String {
        val name = if (language == "fr") nameFr else nameEn.ifBlank { nameFr }
        return listOf(brand, name).filter { it.isNotBlank() }.joinToString(" ")
    }

    fun category(language: String): String = if (language == "fr") categoryFr else categoryEn.ifBlank { categoryFr }

    fun warning(language: String): String = if (language == "fr") warningFr else warningEn.ifBlank { warningFr }
}

class ProductCatalog private constructor(private val productsByBarcode: Map<String, ProductInfo>) {

    fun find(barcode: String): ProductInfo? = productsByBarcode[normalizeBarcode(barcode)]

    fun size(): Int = productsByBarcode.size

    companion object {
        private const val TAG = "ProductCatalog"
        private const val DEFAULT_ASSET = "product_catalog.tsv"

        fun empty(): ProductCatalog = ProductCatalog(emptyMap())

        fun load(context: Context, assetName: String = DEFAULT_ASSET): ProductCatalog =
            runCatching {
                context.assets.open(assetName).bufferedReader().use { reader ->
                    fromTsv(reader.readText())
                }
            }.onFailure {
                PrivacyLog.w(TAG, "Catalogue produit local indisponible")
            }.getOrElse { empty() }

        fun fromTsv(content: String): ProductCatalog {
            val lines = content
                .lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .toList()
            if (lines.isEmpty()) return empty()

            val headers = lines.first().split('\t').map { it.trim() }
            val rows = lines.drop(1)
            val index = headers.withIndex().associate { it.value to it.index }

            fun column(values: List<String>, name: String): String =
                index[name]?.let { values.getOrNull(it) }.orEmpty().trim()

            val products = linkedMapOf<String, ProductInfo>()
            rows.forEach { line ->
                val values = line.split('\t')
                val barcode = normalizeBarcode(column(values, "barcode"))
                val source = column(values, "source")
                val sourceDate = column(values, "source_date")
                val nameFr = column(values, "name_fr")
                if (barcode.isBlank() || nameFr.isBlank() || source.isBlank() || sourceDate.isBlank()) return@forEach
                products.putIfAbsent(
                    barcode,
                    ProductInfo(
                        barcode = barcode,
                        nameFr = nameFr,
                        nameEn = column(values, "name_en"),
                        brand = column(values, "brand"),
                        categoryFr = column(values, "category_fr"),
                        categoryEn = column(values, "category_en"),
                        source = source,
                        sourceDate = sourceDate,
                        warningFr = column(values, "warning_fr"),
                        warningEn = column(values, "warning_en")
                    )
                )
            }
            return ProductCatalog(products)
        }

        private fun normalizeBarcode(value: String): String = value.filter { it.isDigit() }
    }
}

object ProductVoiceFormatter {
    fun build(
        result: VisionAnalysis,
        catalog: ProductCatalog,
        language: String
    ): String? {
        val code = result.barcodes.firstOrNull() ?: return null
        val product = catalog.find(code)
        val category = ProductKnowledgeBase.inferCategory(result)

        return if (language == "fr") {
            buildString {
                if (product != null) {
                    append("Produit trouvé dans le catalogue local : ${product.label("fr")}. ")
                    product.category("fr").takeIf { it.isNotBlank() }?.let {
                        append("Catégorie : $it. ")
                    }
                    append("Source : ${product.source}, date ${product.sourceDate}. ")
                    product.warning("fr").takeIf { it.isNotBlank() }?.let { append("$it ") }
                } else {
                    append("Code détecté : $code. Produit inconnu dans le catalogue local. ")
                    category?.let { append("Catégorie probable d'après l'image ou le texte : ${it.nameFr}. ") }
                }
                if (result.text.isNotBlank()) {
                    append("Texte visible de l'étiquette : ${result.text.take(240)}. ")
                }
                append("Je ne fournis pas de prix, allergènes ou composition sans source vérifiée.")
            }
        } else {
            buildString {
                if (product != null) {
                    append("Product found in the local catalog: ${product.label("en")}. ")
                    product.category("en").takeIf { it.isNotBlank() }?.let {
                        append("Category: $it. ")
                    }
                    append("Source: ${product.source}, date ${product.sourceDate}. ")
                    product.warning("en").takeIf { it.isNotBlank() }?.let { append("$it ") }
                } else {
                    append("Detected code: $code. Product unknown in the local catalog. ")
                    category?.let { append("Likely category from image or text: ${it.nameEn}. ") }
                }
                if (result.text.isNotBlank()) {
                    append("Visible label text: ${result.text.take(240)}. ")
                }
                append("I do not provide price, allergens or ingredients without a verified source.")
            }
        }
    }
}
