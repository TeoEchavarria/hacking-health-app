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
import com.samsung.android.health.sdk.sample.healthdiary.config.YamlConfigManager
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
    var selectedRoutineId by remember { mutableStateOf<String?>(null) }
    var reminderTimes by remember { mutableStateOf<List<HabitReminderTimeEntity>>(emptyList()) }
    var effectiveHabitId by remember { mutableStateOf<String?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRoutineDropdown by remember { mutableStateOf(false) }

    val isNew = habitId == null || habitId == "new"
    val routines = remember { YamlConfigManager.routines }

    LaunchedEffect(habitId) {
        if (!isNew && habitId != null) {
            effectiveHabitId = habitId
            val habit = repository.getHabitById(habitId)
            habit?.let {
                title = it.title
                isEnabled = it.isEnabled
                selectedRoutineId = it.routineId
                reminderTimes = repository.getReminderTimesSync(habitId)
            }
        } else {
            effectiveHabitId = UUID.randomUUID().toString()
            title = ""
            isEnabled = true
            selectedRoutineId = null
            reminderTimes = emptyList()
        }
    }

    fun saveHabit() {
        scope.launch {
            val hid = effectiveHabitId ?: UUID.randomUUID().toString()
            if (title.isBlank()) return@launch
            val habit = HabitEntity(
                habitId = hid, 
                title = title.trim(), 
                isEnabled = isEnabled,
                routineId = selectedRoutineId
            )
            repository.saveHabit(habit, reminderTimes)
            onNavigateBack()
        }
    }

    fun addReminderTime(triggerTime: String, selectedDays: Set<String>) {
        val hid = effectiveHabitId ?: return
        
        // Create a reminder for each selected day
        // If no days selected, create one with null day (daily)
        val newReminders = if (selectedDays.isEmpty()) {
            listOf(HabitReminderTimeEntity(
                reminderId = UUID.randomUUID().toString(),
                habitId = hid,
                triggerTime = triggerTime,
                dayOfWeek = null
            ))
        } else {
            selectedDays.map { day ->
                HabitReminderTimeEntity(
                    reminderId = UUID.randomUUID().toString(),
                    habitId = hid,
                    triggerTime = triggerTime,
                    dayOfWeek = day
                )
            }
        }
        
        reminderTimes = reminderTimes + newReminders
        Log.i("HabitConfig", "Reminder times added $triggerTime for days $selectedDays")
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

            // Routine Selection
            ExposedDropdownMenuBox(
                expanded = showRoutineDropdown,
                onExpandedChange = { showRoutineDropdown = !showRoutineDropdown }
            ) {
                val selectedRoutineName = routines.find { it.routine_id == selectedRoutineId }?.name ?: "No linked routine"
                OutlinedTextField(
                    value = selectedRoutineName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Linked Routine") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoutineDropdown) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showRoutineDropdown,
                    onDismissRequest = { showRoutineDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("No linked routine") },
                        onClick = {
                            selectedRoutineId = null
                            showRoutineDropdown = false
                        }
                    )
                    routines.forEach { routine ->
                        DropdownMenuItem(
                            text = { Text(routine.name) },
                            onClick = {
                                selectedRoutineId = routine.routine_id
                                showRoutineDropdown = false
                            }
                        )
                    }
                }
            }

            Text("Reminder times", style = MaterialTheme.typography.titleMedium)
            reminderTimes.sortedBy { it.triggerTime }.forEach { reminder ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dayLabel = reminder.dayOfWeek?.replaceFirstChar { it.uppercase() } ?: "Daily"
                    AssistChip(
                        onClick = { },
                        label = { Text("${reminder.triggerTime} ($dayLabel)") }
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
            onConfirm = { hour, minute, selectedDays ->
                val triggerTime = "%02d:%02d".format(hour, minute)
                addReminderTime(triggerTime, selectedDays)
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
    onConfirm: (hour: Int, minute: Int, selectedDays: Set<String>) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    
    val daysOfWeek = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    var selectedDays by remember { mutableStateOf(setOf<String>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute, selectedDays)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timePickerState)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Repeat on:", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    daysOfWeek.zip(dayLabels).forEach { (dayKey, label) ->
                        val isSelected = selectedDays.contains(dayKey)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedDays = if (isSelected) {
                                    selectedDays - dayKey
                                } else {
                                    selectedDays + dayKey
                                }
                            },
                            label = { Text(label) },
                            modifier = Modifier.size(36.dp), // Compact size
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                if (selectedDays.isEmpty()) {
                    Text("No days selected = Every day", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    )
}
