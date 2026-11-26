package com.doccheck.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.doccheck.app.data.model.ChatMessage
import com.doccheck.app.data.model.ChatRole
import com.doccheck.app.data.model.Finding
import com.doccheck.app.data.model.ScanResult
import com.doccheck.app.data.model.Source
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoomConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromFindingsList(value: List<Finding>): String = gson.toJson(value)

    @TypeConverter
    fun toFindingsList(value: String): List<Finding> {
        val type = object : TypeToken<List<Finding>>() {}.type
        return runCatching { gson.fromJson<List<Finding>>(value, type) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromSourcesList(value: List<Source>): String = gson.toJson(value)

    @TypeConverter
    fun toSourcesList(value: String): List<Source> {
        val type = object : TypeToken<List<Source>>() {}.type
        return runCatching { gson.fromJson<List<Source>>(value, type) }.getOrDefault(emptyList())
    }
}

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey val id: String,
    val imageUri: String,
    val tamperScore: Int,
    val findings: List<Finding>,
    val timestamp: Long
) {
    fun toDomain() = ScanResult(
        id = id,
        imageUri = imageUri,
        tamperScore = tamperScore,
        findings = findings,
        timestamp = timestamp
    )

    companion object {
        fun fromDomain(model: ScanResult) = ScanResultEntity(
            id = model.id,
            imageUri = model.imageUri,
            tamperScore = model.tamperScore,
            findings = model.findings,
            timestamp = model.timestamp
        )
    }
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val role: String,
    val text: String,
    val sources: List<Source>,
    val timestamp: Long
) {
    fun toDomain() = ChatMessage(
        id = id,
        role = ChatRole.valueOf(role),
        text = text,
        sources = sources,
        timestamp = timestamp,
        isStreaming = false
    )

    companion object {
        fun fromDomain(model: ChatMessage) = ChatMessageEntity(
            id = model.id,
            role = model.role.name,
            text = model.text,
            sources = model.sources,
            timestamp = model.timestamp
        )
    }
}

