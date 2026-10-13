package com.misinversiones.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Investment(
    val name: String = "",
    val category: String = "Otros",
    val link: String = "",
    val amount: Double = 0.0,
    val roi: Double = 0.0,
    val note: String = "",
    val beneficiary: String = "",
    @SerialName("bank_name")
    val bankName: String = "",
    @SerialName("account_number")
    val accountNumber: String = "",
    @SerialName("bic_swift")
    val bicSwift: String = "",
    @SerialName("payment_reference")
    val paymentReference: String = ""
) {
    val profit: Double
        get() = amount * (roi / 100.0)

    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "category" to category,
        "link" to link,
        "amount" to amount,
        "roi" to roi,
        "note" to note,
        "beneficiary" to beneficiary,
        "bank_name" to bankName,
        "account_number" to accountNumber,
        "bic_swift" to bicSwift,
        "payment_reference" to paymentReference
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Investment {
            fun parseDouble(value: Any?): Double {
                return when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }

            fun parseString(vararg keys: String): String {
                for (k in keys) {
                    val v = map[k]
                    if (v is String && v.isNotBlank()) return v
                    if (v != null && v !is Map<*, *> && v !is List<*>) return v.toString()
                }
                return ""
            }

            val cat = parseString("category", "categoria")

            return Investment(
                name = parseString("name", "nombre"),
                category = if (Categories.isValid(cat)) cat else "Otros",
                link = parseString("link", "enlace"),
                amount = parseDouble(map["amount"] ?: map["invertido"] ?: map["cantidad"]),
                roi = parseDouble(map["roi"] ?: map["rentabilidad"]),
                note = parseString("note", "nota"),
                beneficiary = parseString("beneficiary", "beneficiario"),
                bankName = parseString("bank_name", "bankName", "banco"),
                accountNumber = parseString("account_number", "accountNumber", "cuenta"),
                bicSwift = parseString("bic_swift", "bicSwift"),
                paymentReference = parseString("payment_reference", "paymentReference", "referencia")
            )
        }
    }
}
