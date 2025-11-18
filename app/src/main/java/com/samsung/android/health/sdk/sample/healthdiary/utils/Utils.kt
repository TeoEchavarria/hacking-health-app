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
 * Shows a toast with formatted error information
 */
fun showErrorToast(context: Context, exception: Throwable) {
    val errorMessage = getErrorMessage(exception)
    // Limit message length to fit in the toast
    val displayMessage = if (errorMessage.length > 100) {
        errorMessage.substring(0, 97) + "..."
    } else {
        errorMessage
    }
    Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()
    Log.e("[HTK]Utils", "Error shown to user: ${formatError(exception, includeStackTrace = true)}")
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
 * Formats an error with complete information for user display and logging
 * @param exception The exception to format
 * @param includeStackTrace If true, includes the stack trace (useful for debugging)
 * @return Formatted string with error information
 */
fun formatError(exception: Throwable, includeStackTrace: Boolean = false): String {
    val errorInfo = StringBuilder()
    
    // Exception type
    errorInfo.append("Error: ${exception.javaClass.simpleName}\n")
    
    // Error message
    val message = exception.message ?: "No error message"
    errorInfo.append("Message: $message\n")
    
    // Error code if it's ResolvablePlatformException
    if (exception is ResolvablePlatformException) {
        try {
            val errorCode = exception.errorCode
            errorInfo.append("Code: $errorCode\n")
        } catch (e: Exception) {
            // Error code not available
        }
    }
    
    // Error cause
    exception.cause?.let { cause ->
        errorInfo.append("Cause: ${cause.javaClass.simpleName}: ${cause.message}\n")
    }
    
    // Stack trace if requested
    if (includeStackTrace) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        errorInfo.append("\nStack trace:\n${sw.toString()}")
    }
    
    return errorInfo.toString()
}

/**
 * Gets a simplified error message to show to the user
 * @param exception The exception
 * @return String with simplified but informative message
 */
fun getErrorMessage(exception: Throwable): String {
    val message = exception.message ?: "Unknown error"
    
    // If it's ResolvablePlatformException, try to get error code
    if (exception is ResolvablePlatformException) {
        try {
            val errorCode = exception.errorCode
            return "$message (Code: $errorCode)"
        } catch (e: Exception) {
            // Continue with normal message
        }
    }
    
    // If it has a cause, include it
    exception.cause?.let { cause ->
        val causeMessage = cause.message
        if (causeMessage != null && causeMessage != message) {
            return "$message\nCause: $causeMessage"
        }
    }
    
    return message
}

fun resolveException(exception: Throwable, activity: Activity) {
    // Detailed error log before attempting to resolve it
    Log.e("[HTK]Utils", "Resolving exception: ${formatError(exception, includeStackTrace = true)}")
    
    if ((exception is ResolvablePlatformException) && exception.hasResolution) {
        Log.i("[HTK]Utils", "Exception has resolution available, attempting to resolve...")
        exception.resolve(activity)
    } else {
        Log.w("[HTK]Utils", "Exception does not have resolution available or is not ResolvablePlatformException")
    }
}

fun formatString(input: Float): String {
    return String.format(Locale.ENGLISH, "%.2f", input)
}

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy (E)")
    .withZone(ZoneId.systemDefault())
