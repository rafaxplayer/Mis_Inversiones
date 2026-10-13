package com.misinversiones.app.data.model

data class PortfolioDocument(
    val payload: PortfolioPayload = PortfolioPayload(),
    val revision: Long = 0,
    val updatedAt: Long? = null
)
