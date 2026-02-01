package com.example.pricesapp.data

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    val auth: Auth
) {

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            AuthResult.Success
        } catch (e: AuthRestException) {
            when (e.error) {
                "invalid_credentials" ->
                    AuthResult.Error("Correo o contraseña incorrectos")
                else ->
                    AuthResult.Error("Error de autenticación")
            }
        } catch (e: Exception) {
            AuthResult.Error("Error inesperado. Intenta nuevamente")
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }
}
