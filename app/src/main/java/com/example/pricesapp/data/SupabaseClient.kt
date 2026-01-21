package com.example.pricesapp.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime


object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = "https://xqkinudsqmytuqtdyzfy.supabase.co",
        supabaseKey = "sb_publishable_DDLXH7-zxTwDcN-SK3zILA_Gn0kByQt"
    ) {
        install(Postgrest)
        install(Realtime)
    }
}
