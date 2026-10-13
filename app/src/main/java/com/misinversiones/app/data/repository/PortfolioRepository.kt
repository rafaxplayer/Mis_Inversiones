package com.misinversiones.app.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.misinversiones.app.data.model.PortfolioDocument
import com.misinversiones.app.data.model.PortfolioPayload
import com.misinversiones.app.data.util.NameUtils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RevisionConflictException(message: String) : Exception(message)
class DuplicateNameException(message: String) : Exception(message)
class OfflineException(message: String) : Exception(message)

data class PortfolioStreamState(
    val document: PortfolioDocument? = null,
    val isFromCache: Boolean = false,
    val error: String? = null
)

class PortfolioRepository(private val context: Context) {

    private val firestore: FirebaseFirestore?
        get() {
            return try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        }

    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    fun getPortfolioStream(): Flow<PortfolioStreamState> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(PortfolioStreamState(error = "Falta configuración de Firebase (google-services.json)"))
            close()
            return@callbackFlow
        }

        val docRef = db.collection("portfolio").document("current")
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(PortfolioStreamState(error = "Error al conectar con Firestore: ${error.localizedMessage}"))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val map = snapshot.data
                val payloadMap = (map?.get("payload") as? Map<String, Any?>) ?: emptyMap()
                val revision = (map?.get("revision") as? Number)?.toLong() ?: 0L
                val updatedAt = (map?.get("updated_at") as? Timestamp)?.seconds

                val payload = PortfolioPayload.fromMap(payloadMap)
                val doc = PortfolioDocument(
                    payload = payload,
                    revision = revision,
                    updatedAt = updatedAt
                )

                trySend(
                    PortfolioStreamState(
                        document = doc,
                        isFromCache = snapshot.metadata.isFromCache,
                        error = null
                    )
                )
            } else {
                trySend(
                    PortfolioStreamState(
                        document = PortfolioDocument(payload = PortfolioPayload(), revision = 0),
                        isFromCache = false,
                        error = "El documento 'portfolio/current' no existe todavía en Firestore."
                    )
                )
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun updatePortfolioTransaction(
        expectedRevision: Long,
        transform: (PortfolioPayload) -> PortfolioPayload
    ): Result<Unit> {
        val db = firestore
            ?: return Result.failure(IllegalStateException("Firebase no está configurado (falta google-services.json)."))

        if (!isNetworkAvailable()) {
            return Result.failure(
                OfflineException("Sin conexión a Internet. Las modificaciones están deshabilitadas en modo offline.")
            )
        }

        return try {
            val docRef = db.collection("portfolio").document("current")

            db.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val readRevision = if (snapshot.exists()) {
                    (snapshot.get("revision") as? Number)?.toLong() ?: 0L
                } else {
                    0L
                }

                if (snapshot.exists() && readRevision != expectedRevision) {
                    throw RevisionConflictException(
                        "Conflicto de revisión: Los datos han sido actualizados en el servidor (revisión $readRevision vs esperada $expectedRevision). Por favor recarga e reintenta el cambio."
                    )
                }

                val currentPayload = if (snapshot.exists()) {
                    val map = snapshot.data
                    val payloadMap = map?.get("payload") as? Map<String, Any?> ?: emptyMap()
                    PortfolioPayload.fromMap(payloadMap)
                } else {
                    PortfolioPayload()
                }

                val newPayload = transform(currentPayload)

                val validation = NameUtils.validatePayloadNames(newPayload)
                if (validation.isFailure) {
                    val errMessage = validation.exceptionOrNull()?.message ?: "Sondeo de validación de nombres fallido."
                    throw DuplicateNameException(errMessage)
                }

                val newRevision = readRevision + 1
                val updateData = mapOf(
                    "payload" to newPayload.toMap(),
                    "revision" to newRevision,
                    "updated_at" to FieldValue.serverTimestamp()
                )

                if (snapshot.exists()) {
                    transaction.update(docRef, updateData)
                } else {
                    transaction.set(docRef, updateData)
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
