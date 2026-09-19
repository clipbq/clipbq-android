@file:Suppress("SupabaseInternal", "INTERNAL_API_USAGE")

package com.softlabs.clipbq.data

import com.softlabs.clipbq.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClientProvider {
    @Volatile
    private var _client: SupabaseClient? = null
    val client: SupabaseClient
        get() = _client
            ?: error("SupabaseClient has not been initialized. Call initialize(context) first.")

    @OptIn(SupabaseInternal::class)
    fun initialize() {
        if (_client != null) return

        synchronized(this) {
            if (_client == null) {

                _client = createSupabaseClient(
                    supabaseUrl = BuildConfig.SUPABASE_URL,
                    supabaseKey = BuildConfig.SUPABASE_ANON_KEY
                ) {
                    install(Auth) {
                        sessionManager = SettingsSessionManager()

                        alwaysAutoRefresh = true
                        autoLoadFromStorage = true
                    }
                    install(Postgrest)
                    install(Realtime)
                }
            }
        }
    }
}