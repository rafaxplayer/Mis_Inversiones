package com.misinversiones.app.data.export

import com.misinversiones.app.data.model.Categories
import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioPayload
import com.misinversiones.app.data.util.NameUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.Locale

object ImportExportManager {

    val jsonFormatter = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun exportToJson(payload: PortfolioPayload, revision: Long): String {
        val payloadElement = jsonFormatter.encodeToJsonElement(PortfolioPayload.serializer(), payload)
        val rootMap = mapOf(
            "payload" to payloadElement,
            "revision" to JsonPrimitive(revision)
        )
        return jsonFormatter.encodeToString(JsonObject(rootMap))
    }

    fun exportToCsv(payload: PortfolioPayload): String {
        val sb = StringBuilder()
        sb.append("Sección,Nombre,Categoría,Invertido (€),ROI (%),Beneficio (€),Enlace,Nota,Beneficiario,Banco,Número Cuenta,BIC/SWIFT,Referencia\n")

        fun appendInvestments(list: List<Investment>, section: String) {
            list.forEach { item ->
                val cleanName = escapeCsv(item.name)
                val cleanCat = escapeCsv(item.category)
                val amountStr = String.format(Locale.US, "%.2f", item.amount)
                val roiStr = String.format(Locale.US, "%.2f", item.roi)
                val profitStr = String.format(Locale.US, "%.2f", item.profit)
                val cleanLink = escapeCsv(item.link)
                val cleanNote = escapeCsv(item.note)
                val cleanBen = escapeCsv(item.beneficiary)
                val cleanBank = escapeCsv(item.bankName)
                val cleanAcc = escapeCsv(item.accountNumber)
                val cleanBic = escapeCsv(item.bicSwift)
                val cleanRef = escapeCsv(item.paymentReference)

                sb.append("$section,$cleanName,$cleanCat,$amountStr,$roiStr,$profitStr,$cleanLink,$cleanNote,$cleanBen,$cleanBank,$cleanAcc,$cleanBic,$cleanRef\n")
            }
        }

        appendInvestments(payload.investments, "Cartera")
        appendInvestments(payload.reservedInvestments, "Reservas")

        if (payload.banks.isNotEmpty()) {
            sb.append("\nBancos,Nombre,IBAN,BIC/SWIFT,Titular\n")
            payload.banks.forEach { bank ->
                val name = escapeCsv(bank.name)
                val iban = escapeCsv(bank.iban)
                val bic = escapeCsv(bank.bicSwift)
                val holder = escapeCsv(bank.holder)
                sb.append("Banco,$name,$iban,$bic,$holder\n")
            }
        }

        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Validates and parses a JSON string into a PortfolioPayload.
     * Rejects the ENTIRE import if any validation error occurs.
     */
    fun validateAndParseImportJson(jsonString: String): Result<PortfolioPayload> {
        return try {
            val rootElement = jsonFormatter.parseToJsonElement(jsonString)
            if (rootElement !is JsonObject) {
                return Result.failure(IllegalArgumentException("El JSON de importación debe ser un objeto raíz."))
            }

            val payloadObject = if (rootElement.containsKey("payload")) {
                rootElement["payload"]?.jsonObject
                    ?: return Result.failure(IllegalArgumentException("El campo 'payload' no es un objeto JSON válido."))
            } else {
                rootElement
            }

            val payload = jsonFormatter.decodeFromJsonElement(PortfolioPayload.serializer(), payloadObject)

            // Validate categories and amounts
            payload.investments.forEachIndexed { i, inv ->
                if (!Categories.isValid(inv.category)) {
                    return Result.failure(
                        IllegalArgumentException("Inversión '${inv.name}' en posición #$i tiene una categoría no permitida: '${inv.category}'. Categorías válidas: ${Categories.ALL.joinToString()}")
                    )
                }
                if (inv.amount < 0 || !inv.amount.isFinite()) {
                    return Result.failure(
                        IllegalArgumentException("Inversión '${inv.name}' tiene un importe inválido (${inv.amount}).")
                    )
                }
                if (!inv.roi.isFinite()) {
                    return Result.failure(
                        IllegalArgumentException("Inversión '${inv.name}' tiene una rentabilidad (ROI) inválida (${inv.roi}).")
                    )
                }
            }

            payload.reservedInvestments.forEachIndexed { i, inv ->
                if (!Categories.isValid(inv.category)) {
                    return Result.failure(
                        IllegalArgumentException("Reserva '${inv.name}' en posición #$i tiene una categoría no permitida: '${inv.category}'. Categorías válidas: ${Categories.ALL.joinToString()}")
                    )
                }
                if (inv.amount < 0 || !inv.amount.isFinite()) {
                    return Result.failure(
                        IllegalArgumentException("Reserva '${inv.name}' tiene un importe inválido (${inv.amount}).")
                    )
                }
                if (!inv.roi.isFinite()) {
                    return Result.failure(
                        IllegalArgumentException("Reserva '${inv.name}' tiene una rentabilidad (ROI) inválida (${inv.roi}).")
                    )
                }
            }

            // Validate duplication rule across portfolio and reserved
            val nameValidation = NameUtils.validatePayloadNames(payload)
            if (nameValidation.isFailure) {
                return nameValidation.map { payload }
            }

            Result.success(payload)
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("Error al procesar el archivo JSON: ${e.localizedMessage ?: "Formato inválido"}", e))
        }
    }
}
