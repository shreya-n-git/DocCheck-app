package com.doccheck.app.data.repository

import com.doccheck.app.BuildConfig
import com.doccheck.app.data.local.ChatDao
import com.doccheck.app.data.local.ChatMessageEntity
import com.doccheck.app.data.model.ChatMessage
import com.doccheck.app.data.model.ChatRole
import com.doccheck.app.data.model.ScanResult
import com.doccheck.app.data.model.Source
import com.doccheck.app.data.remote.ChatRequestDto
import com.doccheck.app.data.remote.DocCheckApiService
import com.doccheck.app.data.remote.FindingDto
import com.doccheck.app.data.remote.ScanContextDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class ChatStreamEvent {
    data class Token(val partialText: String) : ChatStreamEvent()
    data class Completed(val message: ChatMessage) : ChatStreamEvent()
    data class Error(val errorMessage: String) : ChatStreamEvent()
}

interface ChatRepository {
    fun observeMessages(): Flow<List<ChatMessage>>
    fun sendMessage(
        userText: String,
        scanContext: ScanResult?,
        sessionId: String = "default_session"
    ): Flow<ChatStreamEvent>
    suspend fun clearChat()
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val apiService: DocCheckApiService
) : ChatRepository {

    override fun observeMessages(): Flow<List<ChatMessage>> {
        return chatDao.observeAllMessages().map { list -> list.map { it.toDomain() } }
    }

    override fun sendMessage(
        userText: String,
        scanContext: ScanResult?,
        sessionId: String
    ): Flow<ChatStreamEvent> = flow {
        val now = System.currentTimeMillis()
        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = ChatRole.USER,
            text = userText.trim(),
            timestamp = now
        )
        chatDao.insertMessage(ChatMessageEntity.fromDomain(userMsg))

        if (BuildConfig.USE_MOCK) {
            delay(450)
            val (fullAnswer, sources) = generateMockRagAnswer(userText, scanContext)
            val words = fullAnswer.split(" ")
            val builder = StringBuilder()
            for (i in words.indices) {
                if (i > 0) builder.append(' ')
                builder.append(words[i])
                emit(ChatStreamEvent.Token(builder.toString()))
                delay(28)
            }
            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.ASSISTANT,
                text = fullAnswer,
                sources = sources,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(ChatMessageEntity.fromDomain(assistantMsg))
            emit(ChatStreamEvent.Completed(assistantMsg))
            return@flow
        }

        // TODO: Configure live FastAPI RAG endpoint at BuildConfig.BASE_URL/api/chat
        try {
            val dto = ChatRequestDto(
                sessionId = sessionId,
                message = userText.trim(),
                scanContext = scanContext?.let { scan ->
                    ScanContextDto(
                        scanId = scan.id,
                        tamperScore = scan.tamperScore,
                        findings = scan.findings.map {
                            FindingDto(it.module, it.severity.name, it.description)
                        }
                    )
                }
            )
            val response = apiService.sendChatMessage(dto)
            val sources = response.sources.map { Source(it.title, it.page, it.snippet) }
            val assistantMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = ChatRole.ASSISTANT,
                text = response.answer,
                sources = sources,
                timestamp = System.currentTimeMillis()
            )
            chatDao.insertMessage(ChatMessageEntity.fromDomain(assistantMsg))
            emit(ChatStreamEvent.Completed(assistantMsg))
        } catch (t: Throwable) {
            emit(ChatStreamEvent.Error(t.localizedMessage ?: "Failed to reach DocCheck AI"))
        }
    }

    override suspend fun clearChat() {
        chatDao.clearAllMessages()
    }

    private fun generateMockRagAnswer(
        prompt: String,
        scanContext: ScanResult?
    ): Pair<String, List<Source>> {
        val q = prompt.lowercase()
        return when {
            q.contains("ela") || q.contains("error level") -> {
                "Error Level Analysis (ELA) re-saves an image at a known JPEG quality (typically 90–95%) and measures the pixel-by-pixel compression residual against the original. Authentic regions share a uniform error rate, whereas newly pasted or edited regions—which have undergone fewer compression passes—appear noticeably brighter in the ELA heatmap." to listOf(
                    Source(
                        title = "ELA_guide.pdf",
                        page = 4,
                        snippet = "When a region from a higher-quality source is spliced into a compressed document, its DCT quantization residuals diverge sharply from surrounding background blocks upon re-compression at Q=92."
                    ),
                    Source(
                        title = "JPEG_Forensics_Handbook.pdf",
                        page = 11,
                        snippet = "High-contrast edges naturally exhibit slightly elevated ELA brightness; analysts compare variance across structurally similar text lines rather than isolated edges."
                    )
                )
            }
            q.contains("clone") || q.contains("stamp") || q.contains("copy") -> {
                "Copy-move tampering duplicates pixels from one area of a document to another—often to clone an official stamp, mask an amount, or replicate background texture. DocCheck extracts dense SIFT/ORB keypoints and perceptual block hashes to flag geometrically consistent region pairs even after slight rotation or scaling." to listOf(
                    Source(
                        title = "CopyMove_Forensics.pdf",
                        page = 12,
                        snippet = "RANSAC verification filters out coincidental texture matches and confirms affine-transformed clones across official seals and tabular numerals."
                    ),
                    Source(
                        title = "Stamp_Verification_SOP.pdf",
                        page = 6,
                        snippet = "Genuine wet-ink stamps display micro-variations in paper fiber absorption; identical pixel clusters across two seals indicate digital duplication."
                    )
                )
            }
            q.contains("metadata") || q.contains("exif") || q.contains("audit") -> {
                "A metadata audit inspects EXIF, XMP, and PNG chunk headers for editing software signatures (such as Photoshop, GIMP, or PDF-to-raster converters), mismatched creation vs. modification timestamps, and stripped ICC color profiles that often accompany post-capture tampering." to listOf(
                    Source(
                        title = "EXIF_Tampering_Audit.pdf",
                        page = 7,
                        snippet = "Discrepancies between ExifIFD DateTimeOriginal and XMP ModifyDate or the presence of Adobe XMP History chunks strongly suggest post-capture manipulation."
                    )
                )
            }
            q.contains("flagged") || scanContext != null -> {
                val score = scanContext?.tamperScore ?: 76
                val topFinding = scanContext?.findings?.firstOrNull()?.description
                    ?: "localized compression anomalies and metadata timestamp mismatches."
                "Your attached scan scored $score% tamper probability. The primary trigger was: $topFinding Combined with cross-module checks, this suggests specific areas of the document were modified after the original export. Review the high-severity cards on the report or inspect the ELA and Metadata sources below." to listOf(
                    Source(
                        title = "ELA_guide.pdf",
                        page = 9,
                        snippet = "Composite tamper scores above 50% occur when two or more independent forensic signals—such as localized DCT variance and editing software traces—corroborate."
                    ),
                    Source(
                        title = "EXIF_Tampering_Audit.pdf",
                        page = 3,
                        snippet = "Verify whether the flagged document was re-saved by a messaging app before submission, as re-encoding can alter global quantization tables."
                    )
                )
            }
            else -> {
                "DocCheck combines four forensic signals—Copy-Move detection, Error Level Analysis (ELA), Metadata auditing, and Signature stroke inspection—to score document integrity. Upload a PNG or JPG scan on Home or ask about any specific forensic artifact you'd like explained." to listOf(
                    Source(
                        title = "DocCheck_Forensic_Overview.pdf",
                        page = 2,
                        snippet = "Multi-signal fusion pairs pixel-level residual analysis with container-level metadata verification to reduce false positives on benign scanner noise."
                    )
                )
            }
        }
    }
}

