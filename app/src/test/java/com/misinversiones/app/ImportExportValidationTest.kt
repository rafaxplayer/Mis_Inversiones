package com.misinversiones.app

import com.misinversiones.app.data.export.ImportExportManager
import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportValidationTest {

    @Test
    fun testValidJsonImportAndCsvExport() {
        val payload = PortfolioPayload(
            investments = listOf(
                Investment(
                    name = "Fondo Indexado MSCI",
                    category = "Fondos idx",
                    amount = 5000.0,
                    roi = 7.5,
                    bankName = "MyInvestor"
                )
            )
        )

        val jsonString = ImportExportManager.exportToJson(payload, revision = 5)
        assertTrue(jsonString.contains("Fondo Indexado MSCI"))

        val importResult = ImportExportManager.validateAndParseImportJson(jsonString)
        assertTrue(importResult.isSuccess)

        val importedPayload = importResult.getOrThrow()
        assertEquals(1, importedPayload.investments.size)
        assertEquals("Fondo Indexado MSCI", importedPayload.investments[0].name)
        assertEquals(5000.0, importedPayload.investments[0].amount, 0.001)

        val csvString = ImportExportManager.exportToCsv(importedPayload)
        assertTrue(csvString.contains("Fondo Indexado MSCI"))
        assertTrue(csvString.contains("Fondos idx"))
    }

    @Test
    fun testRejectInvalidCategoryInJson() {
        val invalidJson = """
            {
              "payload": {
                "investments": [
                  {
                    "name": "Cripto Cero",
                    "category": "CategoriaFalsaInexistente",
                    "amount": 100.0,
                    "roi": 2.0
                  }
                ]
              }
            }
        """.trimIndent()

        val result = ImportExportManager.validateAndParseImportJson(invalidJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("categoría no permitida") == true)
    }

    @Test
    fun testRejectNegativeAmountInJson() {
        val invalidJson = """
            {
              "payload": {
                "investments": [
                  {
                    "name": "Prueba Negativa",
                    "category": "Otros",
                    "amount": -50.0,
                    "roi": 1.0
                  }
                ]
              }
            }
        """.trimIndent()

        val result = ImportExportManager.validateAndParseImportJson(invalidJson)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("importe inválido") == true)
    }

    @Test
    fun testRejectDuplicateNamesInJsonImport() {
        val jsonWithDuplicates = """
            {
              "payload": {
                "investments": [
                  { "name": "Deposito Sabadell", "category": "Depositos", "amount": 1000.0 }
                ],
                "reserved_investments": [
                  { "name": "deposito  sabadell", "category": "Depositos", "amount": 500.0 }
                ]
              }
            }
        """.trimIndent()

        val result = ImportExportManager.validateAndParseImportJson(jsonWithDuplicates)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Nombre duplicado") == true)
    }
}
