package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (habitId: String?) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { HabitRepository(context) }
    val habits by repository.getAllHabits().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    fun deleteHabit(habit: HabitEntity) {
        scope.launch {
            repository.deleteHabit(habit)
            habitToDelete = null
            snackbarHostState.showSnackbar("Habit deleted", duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SandboxTopBar(
                title = "Habit Reminders",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add habit")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToEditor(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Habit") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (habits.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            text = "No habits yet.\nAdd a habit to get reminders on your watch.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
            items(habits) { habit ->
                HabitItem(
                    habit = habit,
                    repository = repository,
                    onEdit = { onNavigateToEditor(habit.habitId) },
                    onDelete = { habitToDelete = habit }
                )
            }
        }
    }

    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete habit") },
            text = { Text("Delete \"${habit.title}\"? You will no longer receive reminders.") },
            confirmButton = {
                TextButton(onClick = { deleteHabit(habit) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Formats a reminder time for display, including day of week if specified.
 */
private fun formatReminderTime(reminder: HabitReminderTimeEntity): String {
    val dayOfWeek = reminder.dayOfWeek
    return if (dayOfWeek != null && dayOfWeek.isNotBlank()) {
        val dayDisplay = dayOfWeek.lowercase(Locale.ROOT)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        "$dayDisplay ${reminder.triggerTime}"
    } else {
        reminder.triggerTime
    }
}

@Composable
private fun HabitItem(
    habit: HabitEntity,
    repository: HabitRepository,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val reminderTimes by repository.getReminderTimes(habit.habitId).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = when {
                        reminderTimes.isEmpty() -> "No reminders"
                        else -> reminderTimes.joinToString(", ") { reminder ->
                            formatReminderTime(reminder)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = habit.isEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch {
                            repository.setHabitEnabled(habit.habitId, enabled)
                        }
                    }
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
