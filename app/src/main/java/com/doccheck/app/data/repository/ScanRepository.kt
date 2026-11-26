package com.doccheck.app.data.repository

import android.content.Context
import android.net.Uri
import com.doccheck.app.BuildConfig
import com.doccheck.app.data.local.ScanDao
import com.doccheck.app.data.local.ScanResultEntity
import com.doccheck.app.data.model.Finding
import com.doccheck.app.data.model.ForensicModule
import com.doccheck.app.data.model.HomeStats
import com.doccheck.app.data.model.ScanResult
import com.doccheck.app.data.model.Severity
import com.doccheck.app.data.remote.DocCheckApiService
import com.doccheck.app.util.Constants
import com.doccheck.app.util.FileUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

interface ScanRepository {
    fun observeHomeStats(): Flow<HomeStats>
    fun observeScanHistory(): Flow<List<ScanResult>>
    suspend fun getScanById(id: String): ScanResult?
    suspend fun analyzeDocument(imageUri: Uri, preferredModule: ForensicModule? = null): Result<ScanResult>
    suspend fun clearHistory()
}

@Singleton
class ScanRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanDao: ScanDao,
    private val apiService: DocCheckApiService
) : ScanRepository {

    override fun observeHomeStats(): Flow<HomeStats> {
        return combine(
            scanDao.observeScanCount(),
            scanDao.observeTamperedCount()
        ) { localScans, localTampered ->
            HomeStats(
                scanned = Constants.BASE_SCANNED_COUNT + localScans,
                tampered = Constants.BASE_TAMPERED_COUNT + localTampered,
                accuracy = Constants.BASE_ACCURACY
            )
        }
    }

    override fun observeScanHistory(): Flow<List<ScanResult>> {
        return scanDao.observeAllScans().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getScanById(id: String): ScanResult? {
        return scanDao.getScanById(id)?.toDomain()
    }

    override suspend fun analyzeDocument(
        imageUri: Uri,
        preferredModule: ForensicModule?
    ): Result<ScanResult> {
        // Persist a private cache copy so Coil can display thumbnails across app restarts
        val mime = context.contentResolver.getType(imageUri)?.lowercase() ?: "image/jpeg"
        val ext = if (mime.contains("png")) "png" else "jpg"
        val cachedFile = FileUtils.copyUriToTempFile(context, imageUri, ext)
        val displayUri = cachedFile?.let { Uri.fromFile(it).toString() } ?: imageUri.toString()

        if (BuildConfig.USE_MOCK) {
            delay(1300)
            val result = buildMockScanResult(displayUri, preferredModule)
            scanDao.insertScan(ScanResultEntity.fromDomain(result))
            return Result.success(result)
        }

        // TODO: Point BuildConfig.BASE_URL to your FastAPI backend and set USE_MOCK = false
        return runCatching {
            val uploadFile = cachedFile ?: error("Unable to read selected image file.")
            val body = uploadFile.asRequestBody(mime.toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", uploadFile.name, body)
            val modulePart = preferredModule?.name?.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = apiService.analyzeDocument(part, modulePart)
            val domain = ScanResult(
                id = response.id,
                imageUri = displayUri,
                tamperScore = response.tamperScore,
                findings = response.findings.map { dto ->
                    Finding(
                        module = dto.module,
                        severity = runCatching { Severity.valueOf(dto.severity.uppercase()) }
                            .getOrDefault(Severity.LOW),
                        description = dto.description
                    )
                },
                timestamp = response.timestamp
            )
            scanDao.insertScan(ScanResultEntity.fromDomain(domain))
            domain
        }
    }

    override suspend fun clearHistory() {
        scanDao.clearAllScans()
    }

    private fun buildMockScanResult(imageUri: String, preferredModule: ForensicModule?): ScanResult {
        val seed = abs(imageUri.hashCode() + System.currentTimeMillis().toInt())
        val isFlagged = (seed % 10) >= 3 || preferredModule != null
        val tamperScore = if (isFlagged) 68 + (seed % 26) else 8 + (seed % 14)

        val findings = if (isFlagged) {
            listOf(
                Finding(
                    module = ForensicModule.ELA.displayName,
                    severity = Severity.HIGH,
                    description = "Localized high-frequency compression residuals detected around numeric fields (ΔQ ≈ 18.4%), consistent with post-export splicing."
                ),
                Finding(
                    module = ForensicModule.COPY_MOVE.displayName,
                    severity = if (preferredModule == ForensicModule.COPY_MOVE) Severity.HIGH else Severity.MEDIUM,
                    description = "14 matched SIFT/ORB block clusters found near the official seal region indicating potential stamp duplication."
                ),
                Finding(
                    module = ForensicModule.METADATA.displayName,
                    severity = Severity.MEDIUM,
                    description = "EXIF Software tag references desktop raster editor; CreateDate precedes ModifyDate by 42 minutes."
                ),
                Finding(
                    module = ForensicModule.SIGNATURE.displayName,
                    severity = Severity.LOW,
                    description = "Ink edge anti-aliasing and pressure gradient fall within normal scanned pen variance."
                )
            )
        } else {
            listOf(
                Finding(
                    module = ForensicModule.ELA.displayName,
                    severity = Severity.LOW,
                    description = "Uniform JPEG quantization noise floor across text blocks, background paper, and borders."
                ),
                Finding(
                    module = ForensicModule.COPY_MOVE.displayName,
                    severity = Severity.LOW,
                    description = "No duplicated pixel patches or cloned stamp patterns identified."
                ),
                Finding(
                    module = ForensicModule.METADATA.displayName,
                    severity = Severity.LOW,
                    description = "Consistent camera/scanner ICC profile with intact creation timestamps."
                ),
                Finding(
                    module = ForensicModule.SIGNATURE.displayName,
                    severity = Severity.LOW,
                    description = "Natural paper-fiber ink bleed and continuous stroke curvature verified."
                )
            )
        }

        return ScanResult(
            id = UUID.randomUUID().toString(),
            imageUri = imageUri,
            tamperScore = tamperScore,
            findings = findings,
            timestamp = System.currentTimeMillis()
        )
    }
}

