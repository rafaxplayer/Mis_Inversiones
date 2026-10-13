package com.misinversiones.app.data.util

import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioPayload

enum class SectionType {
    PORTFOLIO,
    RESERVED
}

object NameUtils {

    fun normalizeName(name: String): String {
        return name.trim().replace(Regex("\\s+"), " ").lowercase()
    }

    /**
     * Checks if a name already exists in either portfolio or reserved investments.
     * When editing, [excludeSection] and [excludeIndex] can be supplied to skip checking against self.
     */
    fun isDuplicateName(
        name: String,
        investments: List<Investment>,
        reservedInvestments: List<Investment>,
        excludeSection: SectionType? = null,
        excludeIndex: Int? = null
    ): Boolean {
        val targetNormalized = normalizeName(name)
        if (targetNormalized.isEmpty()) return false

        investments.forEachIndexed { index, item ->
            if (!(excludeSection == SectionType.PORTFOLIO && excludeIndex == index)) {
                if (normalizeName(item.name) == targetNormalized) {
                    return true
                }
            }
        }

        reservedInvestments.forEachIndexed { index, item ->
            if (!(excludeSection == SectionType.RESERVED && excludeIndex == index)) {
                if (normalizeName(item.name) == targetNormalized) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Validates an entire payload to ensure no empty names and no duplicates
     * between investments and reserved_investments.
     */
    fun validatePayloadNames(payload: PortfolioPayload): Result<Unit> {
        val seenNames = mutableMapOf<String, String>() // normalized -> original display name

        fun checkList(list: List<Investment>, sectionLabel: String): Result<Unit>? {
            list.forEachIndexed { index, item ->
                val norm = normalizeName(item.name)
                if (norm.isEmpty()) {
                    return Result.failure(
                        IllegalArgumentException("Elemento #$index en $sectionLabel tiene un nombre vacío o inválido.")
                    )
                }
                if (seenNames.containsKey(norm)) {
                    val existingName = seenNames[norm]
                    return Result.failure(
                        IllegalArgumentException("Nombre duplicado detectado: '$existingName' (coincide con '${item.name}' en $sectionLabel).")
                    )
                }
                seenNames[norm] = item.name
            }
            return null
        }

        checkList(payload.investments, "cartera")?.let { return it }
        checkList(payload.reservedInvestments, "reservas")?.let { return it }

        return Result.success(Unit)
    }
}
