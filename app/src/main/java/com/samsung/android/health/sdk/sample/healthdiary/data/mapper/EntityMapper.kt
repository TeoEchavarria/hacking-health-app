package com.samsung.android.health.sdk.sample.healthdiary.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samsung.android.health.sdk.sample.healthdiary.data.domain.*
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.*

/**
 * Mappers to convert between database entities and domain models
 */
object EntityMapper {
    
    private val gson = Gson()
    
    // ========== SensorBatch Mappers ==========
    
    fun SensorBatchEntity.toDomain(): SensorBatch {
        val samples = try {
            val type = object : TypeToken<List<SensorSample>>() {}.type
            gson.fromJson<List<SensorSample>>(dataJson, type)
        } catch (e: Exception) {
            emptyList()
        }
        
        return SensorBatch(
            id = id,
            timestamp = timestamp,
            sensorType = sensorType,
            samples = samples,
            uploaded = uploaded,
            uploadedAt = uploadedAt,
            receivedAt = receivedAt
        )
    }
    
    fun SensorBatch.toEntity(): SensorBatchEntity {
        return SensorBatchEntity(
            id = id,
            timestamp = timestamp,
            sensorType = sensorType,
            dataJson = gson.toJson(samples),
            sampleCount = samples.size,
            uploaded = uploaded,
            uploadedAt = uploadedAt,
            receivedAt = receivedAt
        )
    }
    
    // ========== UploadLog Mappers ==========
    
    fun UploadLogEntity.toDomain(): UploadLog {
        return UploadLog(
            id = id,
            timestamp = timestamp,
            entityType = EntityType.fromValue(entityType),
            entityId = entityId,
            status = UploadStatus.fromValue(status),
            endpoint = endpoint,
            responseCode = responseCode,
            errorMessage = errorMessage,
            dataSizeBytes = dataSizeBytes,
            durationMs = durationMs
        )
    }
    
    fun UploadLog.toEntity(): UploadLogEntity {
        return UploadLogEntity(
            id = id,
            timestamp = timestamp,
            entityType = entityType.value,
            entityId = entityId,
            status = status.value,
            endpoint = endpoint,
            responseCode = responseCode,
            errorMessage = errorMessage,
            dataSizeBytes = dataSizeBytes,
            durationMs = durationMs
        )
    }
    
    // ========== MedicalDocument Mappers ==========
    
    fun MedicalDocumentEntity.toDomain(): MedicalDocument {
        return MedicalDocument(
            id = id,
            filename = filename,
            filePath = filePath,
            uploadTimestamp = uploadTimestamp,
            fileSize = fileSize,
            mimeType = mimeType,
            description = description,
            processed = processed,
            processedAt = processedAt,
            fileHash = fileHash,
            pageCount = pageCount,
            tags = tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }
    
    fun MedicalDocument.toEntity(): MedicalDocumentEntity {
        return MedicalDocumentEntity(
            id = id,
            filename = filename,
            filePath = filePath,
            uploadTimestamp = uploadTimestamp,
            fileSize = fileSize,
            mimeType = mimeType,
            description = description,
            processed = processed,
            processedAt = processedAt,
            fileHash = fileHash,
            pageCount = pageCount,
            tags = tags.joinToString(",")
        )
    }
    
    // ========== TxAgentQuery Mappers ==========
    
    fun TxAgentQueryEntity.toDomain(): TxAgentQuery {
        return TxAgentQuery(
            id = id,
            timestamp = timestamp,
            queryText = queryText,
            documentId = documentId,
            queryType = QueryType.fromValue(queryType),
            status = QueryStatus.fromValue(status),
            sentAt = sentAt,
            completedAt = completedAt,
            errorMessage = errorMessage,
            userId = userId
        )
    }
    
    fun TxAgentQuery.toEntity(): TxAgentQueryEntity {
        return TxAgentQueryEntity(
            id = id,
            timestamp = timestamp,
            queryText = queryText,
            documentId = documentId,
            queryType = queryType.value,
            status = status.value,
            sentAt = sentAt,
            completedAt = completedAt,
            errorMessage = errorMessage,
            userId = userId
        )
    }
    
    // ========== TxAgentResponse Mappers ==========
    
    fun TxAgentResponseEntity.toDomain(): TxAgentResponse {
        val metadataMap = try {
            metadata?.let {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson<Map<String, Any>>(it, type)
            }
        } catch (e: Exception) {
            null
        }
        
        val sourcesList = try {
            sources?.let {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson<List<String>>(it, type)
            }
        } catch (e: Exception) {
            null
        }
        
        return TxAgentResponse(
            id = id,
            queryId = queryId,
            timestamp = timestamp,
            responseText = responseText,
            metadata = metadataMap,
            confidence = confidence,
            sources = sourcesList,
            userRating = userRating,
            userFeedback = userFeedback,
            processingTimeMs = processingTimeMs,
            modelVersion = modelVersion
        )
    }
    
    fun TxAgentResponse.toEntity(): TxAgentResponseEntity {
        return TxAgentResponseEntity(
            id = id,
            queryId = queryId,
            timestamp = timestamp,
            responseText = responseText,
            metadata = metadata?.let { gson.toJson(it) },
            confidence = confidence,
            sources = sources?.let { gson.toJson(it) },
            userRating = userRating,
            userFeedback = userFeedback,
            processingTimeMs = processingTimeMs,
            modelVersion = modelVersion
        )
    }
}
