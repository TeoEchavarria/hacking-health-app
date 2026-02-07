package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.RoutineEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit,
    onNavigateToPlayer: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val routineDao = remember { db.routineDao() }
    val sessionDao = remember { db.workoutSessionDao() }
    val routines by routineDao.getAllRoutines().collectAsState(initial = emptyList())
    val activeSession by sessionDao.getActiveSession().collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(routines) {
        if (routines.isEmpty()) {
            val rid = "00000000-0000-0000-0000-000000000001"
            val routine = RoutineEntity(routineId = rid, name = "Test Routine")
            val blocks = listOf(
                BlockEntity(blockId = "00000000-0000-0000-0000-000000000002", routineId = rid, exerciseName = "Push-ups", sets = 3, targetWeight = 0f, targetReps = 12, restSec = 30, orderIndex = 0),
                BlockEntity(blockId = "00000000-0000-0000-0000-000000000003", routineId = rid, exerciseName = "Squats", sets = 3, targetWeight = 40f, targetReps = 10, restSec = 30, orderIndex = 1)
            )
            routineDao.saveRoutineWithBlocks(routine, blocks)
        }
    }

    var routineToDelete by remember { mutableStateOf<RoutineEntity?>(null) }

    fun deleteRoutine(routine: RoutineEntity) {
        scope.launch {
            routineDao.deleteRoutineAndBlocks(routine)
            routineToDelete = null
            snackbarHostState.showSnackbar(message = "Routine deleted", duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SandboxTopBar(
                title = "Routines",
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(onClick = { onNavigateToEditor(null) }) {
                        Icon(Icons.Default.Add, contentDescription = "Add routine")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (activeSession != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Finish current workout first",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routines) { routine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToEditor(routine.routineId) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = routine.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { routineToDelete = routine },
                            modifier = Modifier.padding(end = 4.dp).padding(4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete routine")
                        }
                        Button(
                            onClick = { onNavigateToPlayer(routine.routineId) },
                            enabled = activeSession == null,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start")
                        }
                    }
                }
            }
        }
    }

    routineToDelete?.let { routine ->
        AlertDialog(
            onDismissRequest = { routineToDelete = null },
            title = { Text("Delete routine") },
            text = { Text("Delete \"${routine.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteRoutine(routine)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { routineToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
