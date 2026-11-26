package com.doccheck.app.data.repository

import com.doccheck.app.BuildConfig
import com.doccheck.app.data.local.SessionPreferences
import com.doccheck.app.data.model.AuthResult
import com.doccheck.app.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun loginWithEmail(email: String, password: String): AuthResult
    suspend fun signUpWithEmail(fullName: String, email: String, password: String): AuthResult
    suspend fun signInWithGoogle(idToken: String? = null): AuthResult
    suspend fun sendPasswordReset(email: String): Boolean
    suspend fun logout()
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val sessionPreferences: SessionPreferences
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, password: String): AuthResult {
        if (BuildConfig.USE_MOCK) {
            delay(750)
            val displayName = email.substringBefore("@")
                .replace(".", " ")
                .replaceFirstChar { it.uppercase() }
                .ifBlank { "DocCheck Analyst" }
            val user = User(
                id = "mock_${UUID.nameUUIDFromBytes(email.toByteArray())}",
                fullName = displayName,
                email = email.trim()
            )
            sessionPreferences.saveUserSession(user)
            return AuthResult(success = true, user = user)
        }

        // TODO: Add app/google-services.json and enable Firebase Authentication in the Firebase Console.
        return runCatching {
            val auth = FirebaseAuth.getInstance()
            val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user ?: error("Authentication succeeded but user record is missing.")
            val user = User(
                id = fbUser.uid,
                fullName = fbUser.displayName ?: email.substringBefore("@"),
                email = fbUser.email ?: email.trim(),
                photoUrl = fbUser.photoUrl?.toString()
            )
            sessionPreferences.saveUserSession(user)
            AuthResult(success = true, user = user)
        }.getOrElse { err ->
            AuthResult(success = false, errorMessage = err.localizedMessage ?: "Login failed")
        }
    }

    override suspend fun signUpWithEmail(
        fullName: String,
        email: String,
        password: String
    ): AuthResult {
        if (BuildConfig.USE_MOCK) {
            delay(850)
            val user = User(
                id = "mock_${UUID.randomUUID()}",
                fullName = fullName.trim(),
                email = email.trim()
            )
            sessionPreferences.saveUserSession(user)
            return AuthResult(success = true, user = user)
        }

        // TODO: Add app/google-services.json for live Firebase account creation.
        return runCatching {
            val auth = FirebaseAuth.getInstance()
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val fbUser = result.user ?: error("Account creation succeeded but user is missing.")
            val user = User(
                id = fbUser.uid,
                fullName = fullName.trim(),
                email = fbUser.email ?: email.trim()
            )
            sessionPreferences.saveUserSession(user)
            AuthResult(success = true, user = user)
        }.getOrElse { err ->
            AuthResult(success = false, errorMessage = err.localizedMessage ?: "Sign up failed")
        }
    }

    override suspend fun signInWithGoogle(idToken: String?): AuthResult {
        if (BuildConfig.USE_MOCK) {
            delay(650)
            val user = User(
                id = "mock_google_user",
                fullName = "Alex Rivera",
                email = "alex.rivera@doccheck.ai"
            )
            sessionPreferences.saveUserSession(user)
            return AuthResult(success = true, user = user)
        }

        // TODO: Configure Google Sign-In Web Client ID and pass GoogleAuthProvider.getCredential(idToken, null) to FirebaseAuth.
        return AuthResult(
            success = false,
            errorMessage = "Configure Google Sign-In Web Client ID in AuthFragment before disabling USE_MOCK."
        )
    }

    override suspend fun sendPasswordReset(email: String): Boolean {
        if (BuildConfig.USE_MOCK) {
            delay(400)
            return true
        }
        return runCatching {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email.trim()).await()
            true
        }.getOrDefault(false)
    }

    override suspend fun logout() {
        if (!BuildConfig.USE_MOCK) {
            runCatching { FirebaseAuth.getInstance().signOut() }
        }
        sessionPreferences.clearSession()
    }
}

