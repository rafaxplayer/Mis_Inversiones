package com.misinversiones.app.data.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthManager(private val context: Context) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val firebaseAuth: FirebaseAuth?
        get() {
            return try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        }

    init {
        checkFirebaseConfig()
    }

    fun checkFirebaseConfig(): Boolean {
        val auth = firebaseAuth
        if (auth == null) {
            _authState.value = AuthState.Error(
                "Falta la configuración de Firebase (google-services.json). " +
                        "Añade el archivo google-services.json a la carpeta app/ del proyecto."
            )
            return false
        }

        auth.addAuthStateListener { listenerAuth ->
            evaluateUser(listenerAuth.currentUser)
        }
        evaluateUser(auth.currentUser)
        return true
    }

    fun evaluateUser(user: FirebaseUser?) {
        if (firebaseAuth == null) {
            _authState.value = AuthState.Error("Falta la configuración de Firebase (google-services.json).")
            return
        }

        if (user == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        val userEmail = user.email?.trim()?.lowercase() ?: ""
        _authState.value = AuthState.Authenticated(
            email = userEmail,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString()
        )
    }

    fun signInWithGoogleIdToken(idToken: String, onResult: (Result<Unit>) -> Unit) {
        val auth = firebaseAuth
        if (auth == null) {
            onResult(Result.failure(IllegalStateException("Firebase no está configurado (falta google-services.json).")))
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                evaluateUser(result.user)
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                onResult(Result.failure(e))
            }
    }

    fun signOut() {
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val client = GoogleSignIn.getClient(context, gso)
            client.signOut().addOnCompleteListener {
                firebaseAuth?.signOut()
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            firebaseAuth?.signOut()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun isAuthorized(): Boolean {
        return _authState.value is AuthState.Authenticated
    }

    fun getCurrentEmail(): String? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> state.email
            else -> null
        }
    }
}
