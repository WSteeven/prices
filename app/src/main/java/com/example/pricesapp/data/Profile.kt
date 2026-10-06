package com.example.pricesapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    @SerialName("id")
    val id: String,

    @SerialName("email")
    val email: String? = null,

    @SerialName("role")
    val role: String = ROLE_EMPLOYEE
) {
    val isAdmin: Boolean get() = role == ROLE_ADMIN

    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_EMPLOYEE = "empleado"
    }
}
