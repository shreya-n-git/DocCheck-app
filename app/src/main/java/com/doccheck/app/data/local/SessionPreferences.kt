package com.doccheck.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.doccheck.app.data.model.User
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "doccheck_prefs")

@Singleton
class SessionPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHOTO = stringPreferencesKey("user_photo")
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_DONE] ?: false
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_LOGGED_IN] ?: false
    }

    val currentUser: Flow<User?> = context.dataStore.data.map { prefs ->
        val loggedIn = prefs[Keys.IS_LOGGED_IN] ?: false
        if (!loggedIn) null
        else User(
            id = prefs[Keys.USER_ID] ?: "local_user",
            fullName = prefs[Keys.USER_NAME] ?: "DocCheck Analyst",
            email = prefs[Keys.USER_EMAIL] ?: "analyst@doccheck.ai",
            photoUrl = prefs[Keys.USER_PHOTO]
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = completed
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun saveUserSession(user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = true
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_NAME] = user.fullName
            prefs[Keys.USER_EMAIL] = user.email
            if (user.photoUrl != null) {
                prefs[Keys.USER_PHOTO] = user.photoUrl
            } else {
                prefs.remove(Keys.USER_PHOTO)
            }
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LOGGED_IN] = false
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_NAME)
            prefs.remove(Keys.USER_EMAIL)
            prefs.remove(Keys.USER_PHOTO)
        }
    }
}

