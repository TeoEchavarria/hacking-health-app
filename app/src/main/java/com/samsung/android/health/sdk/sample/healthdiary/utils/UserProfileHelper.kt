package com.samsung.android.health.sdk.sample.healthdiary.utils

import com.samsung.android.health.sdk.sample.healthdiary.api.models.UserProfileData
import com.samsung.android.sdk.health.data.data.Field
import com.samsung.android.sdk.health.data.data.UserDataPoint
import com.samsung.android.sdk.health.data.request.DataTypes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object UserProfileHelper {
    
    fun extractProfileData(userDataPoint: UserDataPoint?): UserProfileData? {
        if (userDataPoint == null) return null
        
        return try {
            val allFields = DataTypes.USER_PROFILE.allFields
            val nameField = allFields.find { it.name == "NAME" } as? Field<String>
            val nicknameField = allFields.find { it.name == "NICKNAME" } as? Field<String>
            val emailField = allFields.find { it.name == "EMAIL" } as? Field<String>
            val birthDateField = allFields.find { it.name == "BIRTH_DATE" } as? Field<Instant>
            val genderField = allFields.find { it.name == "GENDER" } as? Field<Int>
            val heightField = allFields.find { it.name == "HEIGHT" } as? Field<Float>
            val weightField = allFields.find { it.name == "WEIGHT" } as? Field<Float>
            
            // Obtener nombre o nickname (nickname tiene prioridad si existe)
            val nickname = nicknameField?.let { getFieldValue(userDataPoint, it) }
            val name = nameField?.let { getFieldValue(userDataPoint, it) } ?: nickname
            val rawEmail = emailField?.let { getFieldValue(userDataPoint, it) }
            
            // TODO: Actualizar esta lógica para usar valores reales del perfil cuando estén disponibles
            // Por ahora, si falta email, generamos uno automáticamente usando el nombre/nickname
            val email = rawEmail?.takeIf { it.isNotBlank() } 
                ?: "${(nickname ?: name ?: "user").lowercase().replace(" ", "")}@example.com"
            
            val birthDate = birthDateField?.let { getFieldValue(userDataPoint, it) }
                ?.let { formatBirthDate(it) } 
                ?: "1990-01-01" // TODO: Usar fecha real cuando esté disponible
            
            val gender = genderField?.let { getFieldValue(userDataPoint, it) }
                ?.let { formatGender(it) }
                ?: "male" // TODO: Usar género real cuando esté disponible
            
            val height = heightField?.let { getFieldValue(userDataPoint, it) }
                ?: 170.0f // TODO: Usar altura real cuando esté disponible (cm)
            
            val weight = weightField?.let { getFieldValue(userDataPoint, it) }
                ?: 70.0f // TODO: Usar peso real cuando esté disponible (kg)
            
            // Solo retornar si tenemos al menos un nombre o nickname
            if (name.isNullOrBlank() && nickname.isNullOrBlank()) {
                null
            } else {
                UserProfileData(
                    name = name ?: nickname ?: "User",
                    email = email,
                    birthDate = birthDate,
                    gender = gender,
                    height = height,
                    weight = weight
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun <T> getFieldValue(userDataPoint: UserDataPoint, field: Field<T>): T? {
        return try {
            userDataPoint.getValue(field)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun formatBirthDate(instant: Instant): String {
        return try {
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
            localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        } ?: ""
    }
    
    private fun formatGender(genderCode: Int): String {
        // Códigos típicos: 0 = Unknown, 1 = Male, 2 = Female
        return when (genderCode) {
            1 -> "male"
            2 -> "female"
            else -> null
        } ?: ""
    }
    
    fun hasMinimumData(profileData: UserProfileData): Boolean {
        // Con solo tener nombre o nickname es suficiente
        return !profileData.name.isNullOrBlank()
    }
}

