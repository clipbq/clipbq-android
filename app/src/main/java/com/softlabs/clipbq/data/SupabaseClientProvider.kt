@file:Suppress("SupabaseInternal", "INTERNAL_API_USAGE")
package com.softlabs.clipbq.data

import android.content.Context
import com.softlabs.clipbq.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.auth.SettingsSessionManager

object SupabaseClientProvider {
    lateinit var client: SupabaseClient
        private set
    @OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)
    fun initialize(context: Context) {
        if (::client.isInitialized) return

        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                // Installs localized session state caching mapping token arrays natively
                sessionManager = SettingsSessionManager()
                alwaysAutoRefresh = true
                autoLoadFromStorage = true
            }
            install(Postgrest)
            install(Realtime)
        }
    }
}