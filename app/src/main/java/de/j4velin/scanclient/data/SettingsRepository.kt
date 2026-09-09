package de.j4velin.scanclient.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The one thing worth remembering between launches: which host to scan from.
 *
 * The migration reads the "prefs" SharedPreferences file the View version wrote, under the same
 * "ip" key, so an existing install keeps the address it was already configured with instead of
 * silently falling back to [SettingsRepository.DEFAULT_IP].
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = "prefs",
                keysToMigrate = setOf(SettingsRepository.IP_KEY_NAME),
            )
        )
    }
)

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val ip: Flow<String> = dataStore.data
        .map { it[IP] ?: DEFAULT_IP }
        .distinctUntilChanged()

    suspend fun updateIp(ip: String) {
        dataStore.edit { it[IP] = ip }
    }

    companion object {
        const val IP_KEY_NAME = "ip"
        const val DEFAULT_IP = "192.168.178.24"

        private val IP = stringPreferencesKey(IP_KEY_NAME)
    }
}
