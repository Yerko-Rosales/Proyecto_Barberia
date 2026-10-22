package com.example.data.supabase

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Central Supabase Client Provider for Big Boys Barbershop Chile.
 * Configured with Supabase PostgREST (PostgreSQL relational queries)
 * and Supabase Realtime (WebSocket reactive synchronization).
 */
object SupabaseClientProvider {

    const val SUPABASE_URL = "https://dgrflhgotxljjiejckoj.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRncmZsaGdvdHhsamppZWpja29qIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTEyOTMxMjEsImV4cCI6MjEwNjg2OTEyMX0.jGudvOPU3tafkt5vHvoSgW0jkSbnBhUsGFy4OuF5fpg"

    @OptIn(ExperimentalSerializationApi::class)
    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(
                Json {
                    encodeDefaults = true
                    ignoreUnknownKeys = true
                    isLenient = true
                    coerceInputValues = true
                    explicitNulls = false
                }
            )
            install(Postgrest)
            install(Realtime)
        }
    }
}

