package com.example.pricesapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Fila de public.user_roles. */
@Serializable
data class UserRole(
    @SerialName("user_id")
    val userId: String,

    @SerialName("role")
    val role: String = ROLE_EMPLOYEE
) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_EMPLOYEE = "empleado"
    }
}

data class Profile(
    val id: String,
    val email: String?,
    val role: String
) {
    val isAdmin: Boolean get() = role == UserRole.ROLE_ADMIN
}
