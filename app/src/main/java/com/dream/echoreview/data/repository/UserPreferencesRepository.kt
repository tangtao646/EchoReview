package com.dream.echoreview.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val apiKey = stringPreferencesKey("api_key")

    val apiKeyFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[apiKey]
    }

    suspend fun updateApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[apiKey] = key
        }
    }
}
