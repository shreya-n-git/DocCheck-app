package com.doccheck.app.data.model

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

data class User(
    val id: String,
    val fullName: String,
    val email: String,
    val photoUrl: String? = null
)

data class AuthResult(
    val success: Boolean,
    val user: User? = null,
    val errorMessage: String? = null
)

enum class Severity {
    LOW,
    MEDIUM,
    HIGH
}

enum class ForensicModule(val displayName: String) {
    COPY_MOVE("Copy-Move"),
    ELA("Error Level (ELA)"),
    METADATA("Metadata Audit"),
    SIGNATURE("Signature Check")
}

@Parcelize
data class Finding(
    val module: String,
    val severity: Severity,
    val description: String
) : Parcelable

@Parcelize
data class ScanResult(
    val id: String,
    val imageUri: String,
    val tamperScore: Int,
    val findings: List<Finding>,
    val timestamp: Long
) : Parcelable {
    val isTampered: Boolean
        get() = tamperScore >= 50
}

@Parcelize
data class Source(
    val title: String,
    val page: Int,
    val snippet: String
) : Parcelable

enum class ChatRole {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val text: String,
    val sources: List<Source> = emptyList(),
    val timestamp: Long,
    val isStreaming: Boolean = false
)

data class HomeStats(
    val scanned: Int,
    val tampered: Int,
    val accuracy: Double
)

data class ForensicToolItem(
    val module: ForensicModule,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val badgeTintRes: Int
)

