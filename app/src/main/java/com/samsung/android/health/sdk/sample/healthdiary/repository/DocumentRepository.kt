package com.samsung.android.health.sdk.sample.healthdiary.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.MedicalDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DocumentRepository(private val context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val documentDao = database.medicalDocumentDao()

    suspend fun saveDocument(uri: Uri): Long = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        
        // Get metadata
        var filename = "unknown.pdf"
        var size = 0L
        
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                
                if (nameIndex != -1) filename = cursor.getString(nameIndex)
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        // Create local file
        val documentsDir = File(context.filesDir, "medical_documents")
        if (!documentsDir.exists()) documentsDir.mkdirs()
        
        val localFile = File(documentsDir, "${System.currentTimeMillis()}_$filename")
        
        // Copy content
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(localFile).use { output ->
                input.copyTo(output)
            }
        }

        // Save to DB
        val entity = MedicalDocumentEntity(
            filename = filename,
            filePath = localFile.absolutePath,
            fileSize = size,
            mimeType = "application/pdf"
        )
        
        documentDao.insert(entity)
    }
}
