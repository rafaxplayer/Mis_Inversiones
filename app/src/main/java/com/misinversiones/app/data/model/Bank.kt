package com.misinversiones.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Bank(
    val name: String = "",
    val iban: String = "",
    @SerialName("bic_swift")
    val bicSwift: String = "",
    val holder: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "iban" to iban,
        "bic_swift" to bicSwift,
        "holder" to holder
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Bank {
            fun parseString(vararg keys: String): String {
                for (k in keys) {
                    val v = map[k]
                    if (v is String && v.isNotBlank()) return v
                    if (v != null && v !is Map<*, *> && v !is List<*>) return v.toString()
                }
                return ""
            }

            return Bank(
                name = parseString("name", "nombre", "banco"),
                iban = parseString("iban"),
                bicSwift = parseString("bic_swift", "bicSwift"),
                holder = parseString("holder", "titular")
            )
        }
    }
}
