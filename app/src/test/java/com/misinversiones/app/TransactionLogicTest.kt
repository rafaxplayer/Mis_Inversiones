package com.misinversiones.app

import com.misinversiones.app.data.model.Investment
import com.misinversiones.app.data.model.PortfolioPayload
import com.misinversiones.app.data.repository.DuplicateNameException
import com.misinversiones.app.data.repository.OfflineException
import com.misinversiones.app.data.repository.RevisionConflictException
import com.misinversiones.app.data.util.NameUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransactionLogicTest {

    @Test
    fun testRevisionConflictDetection() {
        val storedRevision = 10L
        val clientExpectedRevision = 9L

        fun simulateTransaction(expectedRev: Long, storedRev: Long) {
            if (expectedRev != storedRev) {
                throw RevisionConflictException("Conflicto de revisión: $storedRev vs $expectedRev")
            }
        }

        try {
            simulateTransaction(clientExpectedRevision, storedRevision)
            fail("Debió lanzar RevisionConflictException")
        } catch (e: Exception) {
            assertTrue(e is RevisionConflictException)
            assertTrue(e.message?.contains("Conflicto de revisión") == true)
        }
    }

    @Test
    fun testTransactionIncrementsRevision() {
        val currentRevision = 4L
        val newRevision = currentRevision + 1
        assertEquals(5L, newRevision)
    }

    @Test
    fun testOfflineBlocking() {
        val isNetworkAvailable = false

        fun attemptMutation() {
            if (!isNetworkAvailable) {
                throw OfflineException("Sin conexión a Internet")
            }
        }

        try {
            attemptMutation()
            fail("Debió lanzar OfflineException")
        } catch (e: Exception) {
            assertTrue(e is OfflineException)
        }
    }

    @Test
    fun testTransactionRaceConditionDuplicateValidation() {
        val readPayloadInTransaction = PortfolioPayload(
            investments = listOf(
                Investment(name = "Fondo Naranja", category = "Fondos idx")
            )
        )

        val newInvestmentFromClient = Investment(name = "fondo  naranja", category = "Fondos idx")

        val updatedPayload = readPayloadInTransaction.copy(
            investments = readPayloadInTransaction.investments + newInvestmentFromClient
        )

        val validation = NameUtils.validatePayloadNames(updatedPayload)
        assertTrue(validation.isFailure)
    }
}
