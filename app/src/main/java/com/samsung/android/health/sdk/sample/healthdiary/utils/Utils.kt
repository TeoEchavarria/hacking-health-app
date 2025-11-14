/*
 * Copyright (C) 2024 Samsung Electronics Co., Ltd. All rights reserved
 */
package com.samsung.android.health.sdk.sample.healthdiary.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.samsung.android.health.sdk.sample.healthdiary.R
import com.samsung.android.sdk.health.data.error.ResolvablePlatformException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.CoroutineExceptionHandler

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/**
 * Muestra un toast con información de error formateada
 */
fun showErrorToast(context: Context, exception: Throwable) {
    val errorMessage = getErrorMessage(exception)
    // Limitar longitud del mensaje para que quepa en el toast
    val displayMessage = if (errorMessage.length > 100) {
        errorMessage.substring(0, 97) + "..."
    } else {
        errorMessage
    }
    Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()
    Log.e("[HTK]Utils", "Error mostrado al usuario: ${formatError(exception, includeStackTrace = true)}")
}

fun showDatePickerDialogueBox(
    activity: Activity,
    startDate: LocalDateTime,
    callback: (LocalDateTime) -> Unit
) {
    var newStartDate = startDate
    val customLayout: View = activity.layoutInflater.inflate(R.layout.calender_view, null)
    val selectDatePicker = customLayout.findViewById<DatePicker>(R.id.date_picker)
    selectDatePicker.maxDate = Calendar.getInstance().timeInMillis

    selectDatePicker.init(
        startDate.year,
        startDate.monthValue - 1,
        startDate.dayOfMonth
    ) { _, year, month, day ->
        val actualMonth = month + 1
        val updatedMonth = if (actualMonth < 10) "0$actualMonth" else actualMonth
        val updatedDay = if (day < 10) "0$day" else day
        newStartDate = LocalDateTime.parse(
            "$year-$updatedMonth-$updatedDay 00:00",
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
        )
    }

    AlertDialog.Builder(activity)
        .setTitle(R.string.date_picker_title)
        .setView(customLayout)
        .setPositiveButton(R.string.date_picker_confirm_button) { _, _ ->
            callback(newStartDate)
        }
        .create()
        .show()
}

/**
 * Formatea un error con información completa para mostrar al usuario y logging
 * @param exception La excepción a formatear
 * @param includeStackTrace Si true, incluye el stack trace (útil para debugging)
 * @return String formateado con información del error
 */
fun formatError(exception: Throwable, includeStackTrace: Boolean = false): String {
    val errorInfo = StringBuilder()
    
    // Tipo de excepción
    errorInfo.append("Error: ${exception.javaClass.simpleName}\n")
    
    // Mensaje del error
    val message = exception.message ?: "Sin mensaje de error"
    errorInfo.append("Mensaje: $message\n")
    
    // Código de error si es ResolvablePlatformException
    if (exception is ResolvablePlatformException) {
        try {
            val errorCode = exception.errorCode
            errorInfo.append("Código: $errorCode\n")
        } catch (e: Exception) {
            // Error code no disponible
        }
    }
    
    // Causa del error
    exception.cause?.let { cause ->
        errorInfo.append("Causa: ${cause.javaClass.simpleName}: ${cause.message}\n")
    }
    
    // Stack trace si se solicita
    if (includeStackTrace) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        errorInfo.append("\nStack trace:\n${sw.toString()}")
    }
    
    return errorInfo.toString()
}

/**
 * Obtiene un mensaje de error simplificado para mostrar al usuario
 * @param exception La excepción
 * @return String con mensaje simplificado pero informativo
 */
fun getErrorMessage(exception: Throwable): String {
    val message = exception.message ?: "Error desconocido"
    
    // Si es ResolvablePlatformException, intentar obtener código de error
    if (exception is ResolvablePlatformException) {
        try {
            val errorCode = exception.errorCode
            return "$message (Código: $errorCode)"
        } catch (e: Exception) {
            // Continuar con mensaje normal
        }
    }
    
    // Si tiene causa, incluirla
    exception.cause?.let { cause ->
        val causeMessage = cause.message
        if (causeMessage != null && causeMessage != message) {
            return "$message\nCausa: $causeMessage"
        }
    }
    
    return message
}

fun resolveException(exception: Throwable, activity: Activity) {
    // Log detallado del error antes de intentar resolverlo
    Log.e("[HTK]Utils", "Resolviendo excepción: ${formatError(exception, includeStackTrace = true)}")
    
    if ((exception is ResolvablePlatformException) && exception.hasResolution) {
        Log.i("[HTK]Utils", "Excepción tiene resolución disponible, intentando resolver...")
        exception.resolve(activity)
    } else {
        Log.w("[HTK]Utils", "Excepción no tiene resolución disponible o no es ResolvablePlatformException")
    }
}

fun formatString(input: Float): String {
    return String.format(Locale.ENGLISH, "%.2f", input)
}

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy (E)")
    .withZone(ZoneId.systemDefault())
