package com.vpt.scout.proximity

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProximityAlertPreferences(context: Context) {

    private val dataStore = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("proximity_alerts.preferences_pb") }
    )

    val settings: Flow<ProximityAlertSettings> = dataStore.data.map { prefs ->
        ProximityAlertSettings(
            enabled = prefs[Keys.ENABLED] ?: false,
            thresholdFeet = prefs[Keys.THRESHOLD_FEET] ?: 500
        )
    }

    val suppressionState: Flow<AlertSuppressionState> = dataStore.data.map { prefs ->
        AlertSuppressionState(
            lastAlertedApn = prefs[Keys.LAST_ALERTED_APN],
            lastInsideThreshold = prefs[Keys.LAST_INSIDE_THRESHOLD] ?: false
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ENABLED] = enabled
        }
    }

    suspend fun setThresholdFeet(feet: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.THRESHOLD_FEET] = feet
        }
    }

    suspend fun updateSuppression(state: AlertSuppressionState) {
        dataStore.edit { prefs ->
            if (state.lastAlertedApn == null) {
                prefs.remove(Keys.LAST_ALERTED_APN)
            } else {
                prefs[Keys.LAST_ALERTED_APN] = state.lastAlertedApn
            }
            prefs[Keys.LAST_INSIDE_THRESHOLD] = state.lastInsideThreshold
        }
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("proximity_enabled")
        val THRESHOLD_FEET = intPreferencesKey("proximity_threshold_feet")
        val LAST_ALERTED_APN = stringPreferencesKey("proximity_last_alerted_apn")
        val LAST_INSIDE_THRESHOLD = booleanPreferencesKey("proximity_last_inside_threshold")
    }
}
