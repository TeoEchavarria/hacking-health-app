package com.samsung.android.health.sdk.sample.healthdiary.update.domain.usecase

import java.io.File
import java.security.MessageDigest
import javax.inject.Inject

class VerifyUpdateUseCase @Inject constructor() {

    suspend operator fun invoke(file: File, expectedSha256: String): Boolean {
        if (!file.exists()) return false
        
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            hash.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
