package com.example.pricesapp.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.Storage

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://fjmxqlpvedhfuydkhwiq.supabase.co",
        supabaseKey = "sb_publishable_Pj22Jl8gYO1De78pi941Fg_3RTCSzO2"
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
        install(Auth) {
            autoLoadFromStorage = true
            autoSaveToStorage = true
            alwaysAutoRefresh = true
        }
    }

    // Usamos 'Auth' en lugar de 'GoTrue' y 'auth' en lugar de 'gotrue'
    val auth: Auth = client.auth
    val postgrest: Postgrest = client.postgrest
    val realtime: Realtime = client.realtime
}