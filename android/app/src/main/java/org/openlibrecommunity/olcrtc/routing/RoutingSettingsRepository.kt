package org.openlibrecommunity.olcrtc.routing

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class RoutingSettingsRepository private constructor(
    private val appContext: Context,
) {
    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile(dataStoreName) },
    )

    val settings: Flow<RoutingSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences ->
            RoutingSettings(
                mode = RoutingMode.fromStorageValue(preferences[routingModeKey]),
                selectedPackages = preferences[selectedPackagesKey].orEmpty(),
                onboardingCompleted = preferences[onboardingCompletedKey] ?: false,
                recommendationSeeded = preferences[recommendationSeededKey] ?: false,
            )
        }

    suspend fun save(mode: RoutingMode, selectedPackages: Set<String>) {
        dataStore.edit { preferences ->
            preferences[routingModeKey] = mode.storageValue
            preferences[selectedPackagesKey] = sanitizePackages(selectedPackages)
        }
    }

    suspend fun completeOnboarding(mode: RoutingMode, selectedPackages: Set<String>) {
        dataStore.edit { preferences ->
            preferences[routingModeKey] = mode.storageValue
            preferences[selectedPackagesKey] = sanitizePackages(selectedPackages)
            preferences[onboardingCompletedKey] = true
            preferences[recommendationSeededKey] = true
        }
    }

    suspend fun seedRecommendationsIfNeeded(recommendedPackages: Set<String>) {
        dataStore.edit { preferences ->
            if (preferences[recommendationSeededKey] == true) {
                return@edit
            }

            if (preferences[selectedPackagesKey].isNullOrEmpty()) {
                preferences[selectedPackagesKey] = sanitizePackages(recommendedPackages)
            }
            preferences[recommendationSeededKey] = true
        }
    }

    companion object {
        private const val dataStoreName = "routing_settings"
        private val routingModeKey = stringPreferencesKey("routing_mode")
        private val selectedPackagesKey = stringSetPreferencesKey("selected_packages")
        private val onboardingCompletedKey = booleanPreferencesKey("routing_onboarding_completed")
        private val recommendationSeededKey = booleanPreferencesKey("routing_recommendation_seeded")

        @Volatile
        private var instance: RoutingSettingsRepository? = null

        fun getInstance(context: Context): RoutingSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: RoutingSettingsRepository(context.applicationContext).also { instance = it }
            }
        }

        private fun sanitizePackages(packages: Set<String>): Set<String> {
            return packages.mapNotNull { value ->
                value.trim().takeIf { it.isNotEmpty() }
            }.toSet()
        }
    }
}
