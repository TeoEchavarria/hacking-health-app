package com.samsung.android.health.sdk.sample.healthdiary.views

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitEntity
import com.samsung.android.health.sdk.sample.healthdiary.data.room.entity.HabitReminderTimeEntity
import com.samsung.android.health.sdk.sample.healthdiary.habit.HabitRepository
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    habitId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HabitRepository(context) }
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(true) }
    var reminderTimes by remember { mutableStateOf<List<HabitReminderTimeEntity>>(emptyList()) }
    var effectiveHabitId by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isNew = habitId == null || habitId == "new"

    LaunchedEffect(habitId) {
        if (!isNew && habitId != null) {
            effectiveHabitId = habitId
            val habit = repository.getHabitById(habitId)
            habit?.let {
                title = it.title
                isEnabled = it.isEnabled
                reminderTimes = repository.getReminderTimesSync(habitId)
            }
        } else {
            effectiveHabitId = UUID.randomUUID().toString()
            title = ""
            isEnabled = true
            reminderTimes = emptyList()
        }
    }

    fun saveHabit() {
        scope.launch {
            val hid = effectiveHabitId ?: UUID.randomUUID().toString()
            if (title.isBlank()) return@launch
            val habit = HabitEntity(habitId = hid, title = title.trim(), isEnabled = isEnabled)
            repository.saveHabit(habit, reminderTimes)
            onNavigateBack()
        }
    }

    fun addReminderTime(triggerTime: String) {
        val hid = effectiveHabitId ?: return
        val newReminder = HabitReminderTimeEntity(
            reminderId = UUID.randomUUID().toString(),
            habitId = hid,
            triggerTime = triggerTime
        )
        reminderTimes = reminderTimes + newReminder
        Log.i("HabitConfig", "Reminder time added $triggerTime")
    }

    fun removeReminderTime(reminder: HabitReminderTimeEntity) {
        reminderTimes = reminderTimes.filter { it.reminderId != reminder.reminderId }
    }

    fun deleteHabit() {
        scope.launch {
            val hid = effectiveHabitId ?: return@launch
            val habit = repository.getHabitById(hid) ?: return@launch
            repository.deleteHabit(habit)
            showDeleteConfirm = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            SandboxTopBar(
                title = if (isNew) "New Habit" else "Edit Habit",
                onNavigationClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Habit title") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isEnabled, onCheckedChange = { isEnabled = it })
            }

            Text("Reminder times", style = MaterialTheme.typography.titleMedium)
            reminderTimes.forEach { reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text(reminder.triggerTime) }
                    )
                    IconButton(onClick = { removeReminderTime(reminder) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove time")
                    }
                }
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add time")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { saveHabit() }, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
            if (!isNew) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete habit", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showTimePicker) {
        HabitClockTimePickerDialog(
            initialHour = 9,
            initialMinute = 0,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                val triggerTime = "%02d:%02d".format(hour, minute)
                addReminderTime(triggerTime)
                showTimePicker = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete habit") },
            text = { Text("Delete \"$title\"? You will no longer receive reminders.") },
            confirmButton = {
                TextButton(onClick = { deleteHabit() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HabitClockTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
