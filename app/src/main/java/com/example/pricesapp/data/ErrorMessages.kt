package com.example.pricesapp.data

import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import java.io.IOException

/** Convierte excepciones de Supabase/red en mensajes claros para el usuario. */
fun Throwable.toUserMessage(action: String): String = when (this) {
    is PostgrestRestException -> when (code) {
        "42501" -> "No tienes permiso para $action"
        "23505" -> "Ya existe un producto con ese código de barras"
        "23503" -> "El dato está en uso y no se puede $action"
        "PGRST301", "PGRST303" -> "Tu sesión expiró, vuelve a iniciar sesión"
        else -> "No se pudo $action: ${message ?: "error del servidor"}"
    }
    is RestException -> when (statusCode) {
        401 -> "Tu sesión expiró, vuelve a iniciar sesión"
        403 -> "No tienes permiso para $action"
        else -> "No se pudo $action (error ${statusCode})"
    }
    is HttpRequestException, is IOException -> "Sin conexión. Revisa tu internet e intenta de nuevo"
    else -> "No se pudo $action. Intenta nuevamente"
}
