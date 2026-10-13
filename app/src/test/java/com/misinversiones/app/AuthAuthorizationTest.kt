package com.misinversiones.app

import com.misinversiones.app.data.auth.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthAuthorizationTest {

    @Test
    fun testEmailAuthorizationLogic() {
        val ownerEmail = "propietario@ejemplo.com"

        fun evaluate(userEmail: String?): AuthState {
            if (userEmail == null) return AuthState.Unauthenticated
            val normUser = userEmail.trim().lowercase()
            val normOwner = ownerEmail.trim().lowercase()
            return if (normOwner.isNotEmpty() && normUser != normOwner) {
                AuthState.Unauthorized(normUser, "Correo no autorizado")
            } else {
                AuthState.Authenticated(normUser, "Propietario", null)
            }
        }

        // Matching email
        val state1 = evaluate("PROPIETARIO@EJEMPLO.COM ")
        assertTrue(state1 is AuthState.Authenticated)
        assertEquals("propietario@ejemplo.com", (state1 as AuthState.Authenticated).email)

        // Non-matching email
        val state2 = evaluate("intruso@ejemplo.com")
        assertTrue(state2 is AuthState.Unauthorized)
        assertEquals("intruso@ejemplo.com", (state2 as AuthState.Unauthorized).email)

        // Unauthenticated
        val state3 = evaluate(null)
        assertTrue(state3 is AuthState.Unauthenticated)
    }
}
