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

    fun isValid(category: String): Boolean = ALL.contains(category)
}
