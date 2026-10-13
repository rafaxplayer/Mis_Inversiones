package com.misinversiones.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PortfolioPayload(
    val investments: List<Investment> = emptyList(),
    val banks: List<Bank> = emptyList(),
    @SerialName("reserved_investments")
    val reservedInvestments: List<Investment> = emptyList()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "investments" to investments.map { it.toMap() },
        "banks" to banks.map { it.toMap() },
        "reserved_investments" to reservedInvestments.map { it.toMap() }
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): PortfolioPayload {
            val invList = (map["investments"] as? List<*>)
                ?.mapNotNull { item ->
                    (item as? Map<String, Any?>)?.let { Investment.fromMap(it) }
                } ?: emptyList()

            val bankList = (map["banks"] as? List<*>)
                ?.mapNotNull { item ->
                    (item as? Map<String, Any?>)?.let { Bank.fromMap(it) }
                } ?: emptyList()

            val resList = (map["reserved_investments"] as? List<*>)
                ?.mapNotNull { item ->
                    (item as? Map<String, Any?>)?.let { Investment.fromMap(it) }
                } ?: emptyList()

            return PortfolioPayload(
                investments = invList,
                banks = bankList,
                reservedInvestments = resList
            )
        }
    }
}
