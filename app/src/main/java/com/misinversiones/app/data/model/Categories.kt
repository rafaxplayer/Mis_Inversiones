package com.misinversiones.app.data.model

object Categories {
    val ALL = listOf(
        "Otros",
        "Acciones",
        "ETF",
        "Fondos idx",
        "CrowdLending",
        "C. Remunerada",
        "Depositos"
    )

    val ISIN_SUPPORTED = setOf(
        "Fondos idx",
        "ETF",
        "Acciones"
    )

    fun isValid(category: String): Boolean = ALL.contains(category)

    fun supportsIsin(category: String): Boolean = ISIN_SUPPORTED.contains(category)
}
