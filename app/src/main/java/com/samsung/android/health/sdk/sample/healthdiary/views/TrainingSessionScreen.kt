package com.samsung.android.health.sdk.sample.healthdiary.views

import android.Manifest
import android.util.Log
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.training.*
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.Routine
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.Segment
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.SegmentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

private const val WORKOUT_PROTOCOL_PHONE_TAG = "WorkoutProtocolPhone"
private const val WORKOUT_START_PATH = "/workout/start"
private const val WORKOUT_ACK_PATH = "/workout/ack"
private const val ACK_TIMEOUT_MS = 2000L

private enum class WorkoutStartState { Idle, Connecting, Started, Error }

/** ACK payload from watch: { routineId, status, reason?, at } — status is STARTED | REJECTED */
private data class WorkoutAckPayload(
    @SerializedName("routineId") val routineId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("at") val at: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSessionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val stateManager = remember { TrainingStateManager(context) }
    val reminderScheduler = remember { TrainingReminderScheduler(context) }
    val scope = rememberCoroutineScope()
    
    var state by remember { mutableStateOf(stateManager.getTodayState()) }
    var showReminderSettings by remember { mutableStateOf(false) }
    
    // Workout start: UI state and ACK handling (phone trusts only ACK; no assumption from notification/service)
    var workoutStartState by remember { mutableStateOf(WorkoutStartState.Idle) }
    val snackbarHostState = remember { SnackbarHostState() }
    val ackReceivedFlow = remember { MutableSharedFlow<Pair<String, String?>>() } // status to (reason?)
    val pendingAckRoutineId = remember { MutableStateFlow<String?>(null) }
    
    // Register MessageClient listener for /workout/ack only
    DisposableEffect(context) {
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            if (event.path != WORKOUT_ACK_PATH) return@OnMessageReceivedListener
            try {
                val json = String(event.data, Charsets.UTF_8)
                val ack = Gson().fromJson(json, WorkoutAckPayload::class.java)
                val routineId = ack.routineId
                val status = ack.status
                if (routineId != null && status != null && routineId == pendingAckRoutineId.value) {
                    Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "RX /workout/ack status=$status routineId=$routineId")
                    pendingAckRoutineId.value = null
                    ackReceivedFlow.tryEmit(status to ack.reason)
                }
            } catch (e: Exception) {
                Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Failed to parse workout ack", e)
            }
        }
        messageClient.addListener(listener)
        onDispose { messageClient.removeListener(listener) }
    }
    
    // Request notification permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderScheduler.scheduleAllReminders()
        }
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminderScheduler.scheduleAllReminders()
        }
    }
    
    // Refresh state when needed
    fun refreshState() {
        state = stateManager.getTodayState()
    }
    
    fun buildDefaultRoutine(): Routine = Routine(
        routineId = UUID.randomUUID().toString(),
        startAt = Instant.now().toString(),
        segments = listOf(
            Segment(SegmentType.WORK, "Push-ups", 30),
            Segment(SegmentType.REST, "Rest", 15),
            Segment(SegmentType.WORK, "Squats", 30),
            Segment(SegmentType.REST, "Rest", 15)
        )
    )
    
    fun startWorkoutNow() {
        scope.launch {
            workoutStartState = WorkoutStartState.Connecting
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            Log.d(WORKOUT_PROTOCOL_PHONE_TAG, "Connected nodes: ${nodes.size}")
            if (nodes.isEmpty()) {
                workoutStartState = WorkoutStartState.Error
                snackbarHostState.showSnackbar(
                    "No watch connected. Make sure Bluetooth is on and the watch is paired.",
                    duration = SnackbarDuration.Long
                )
                return@launch
            }
            val routine = buildDefaultRoutine()
            val jsonPayload = Gson().toJson(routine)
            pendingAckRoutineId.value = routine.routineId
            Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "TX /workout/start routineId=${routine.routineId}")
            val messageClient = Wearable.getMessageClient(context)
            var sendOk = false
            for (node in nodes) {
                try {
                    messageClient.sendMessage(node.id, WORKOUT_START_PATH, jsonPayload.toByteArray(Charsets.UTF_8)).await()
                    sendOk = true
                } catch (e: Exception) {
                    Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Send failed to node ${node.displayName}", e)
                }
            }
            if (!sendOk) {
                workoutStartState = WorkoutStartState.Error
                pendingAckRoutineId.value = null
                snackbarHostState.showSnackbar("Failed to send to watch.", duration = SnackbarDuration.Short)
                return@launch
            }
            val ackResult = withTimeoutOrNull(ACK_TIMEOUT_MS) { ackReceivedFlow.first() }
            if (ackResult != null) {
                val (status, reason) = ackResult
                when (status) {
                    "STARTED" -> {
                        workoutStartState = WorkoutStartState.Started
                    }
                    "REJECTED" -> {
                        workoutStartState = WorkoutStartState.Error
                        snackbarHostState.showSnackbar(
                            reason?.takeIf { it.isNotBlank() } ?: "Watch rejected the workout.",
                            duration = SnackbarDuration.Long
                        )
                    }
                    else -> {
                        workoutStartState = WorkoutStartState.Error
                    }
                }
            } else {
                workoutStartState = WorkoutStartState.Error
                pendingAckRoutineId.value = null
                Log.w(WORKOUT_PROTOCOL_PHONE_TAG, "ACK timeout routineId=${routine.routineId}")
                snackbarHostState.showSnackbar(
                    "Watch started but did not confirm. Open watch to continue.",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SandboxTopBar(
                title = "Daily Training Session",
                onNavigationClick = onNavigateBack,
                actions = {
                    SandboxIconButton(
                        icon = Icons.Default.Notifications,
                        onClick = { showReminderSettings = true },
                        contentDescription = "Reminder Settings"
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Progress Card
            DailyProgressCard(state, modifier = Modifier.fillMaxWidth())
            
            // Start workout button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { startWorkoutNow() },
                    enabled = workoutStartState == WorkoutStartState.Idle || workoutStartState == WorkoutStartState.Error,
                    modifier = Modifier.weight(1f)
                ) {
                    if (workoutStartState == WorkoutStartState.Connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = when (workoutStartState) {
                            WorkoutStartState.Connecting -> "Connecting to watch…"
                            WorkoutStartState.Started -> "Started"
                            else -> "Start workout"
                        }
                    )
                }
            }
            
            // Block A - Cardio Base
            BlockACard(
                blockA = state.blockA,
                onModeChange = { mode ->
                    val newBlockA = state.blockA.copy(selectedMode = mode)
                    stateManager.updateBlockA(newBlockA)
                    refreshState()
                },
                onSegmentComplete = { segmentName ->
                    val newSegments = if (state.blockA.completedSegments.contains(segmentName)) {
                        state.blockA.completedSegments - segmentName
                    } else {
                        state.blockA.completedSegments + segmentName
                    }
                    val option = when (state.blockA.selectedMode) {
                        CardioMode.CYCLING -> BlockA.CYCLING_OPTION
                        CardioMode.RUNNING -> BlockA.RUNNING_OPTION
                        CardioMode.SWIMMING -> BlockA.SWIMMING_OPTION
                    }
                    val allCompleted = option.segments.all { newSegments.contains(it.name) }
                    val newBlockA = state.blockA.copy(
                        completedSegments = newSegments,
                        isCompleted = allCompleted
                    )
                    stateManager.updateBlockA(newBlockA)
                    refreshState()
                },
                onBlockComplete = { completed ->
                    val newBlockA = state.blockA.copy(isCompleted = completed)
                    stateManager.updateBlockA(newBlockA)
                    refreshState()
                }
            )
            
            // Block B - Lower Body + Core
            BlockBCard(
                blockB = state.blockB,
                onExerciseToggle = { exerciseId ->
                    stateManager.toggleExercise(BlockType.B, exerciseId)
                    refreshState()
                },
                onRestDurationChange = { seconds ->
                    val newBlockB = state.blockB.copy(restDurationSeconds = seconds)
                    stateManager.updateBlockB(newBlockB)
                    refreshState()
                },
                onBlockComplete = { completed ->
                    val newBlockB = state.blockB.copy(isCompleted = completed)
                    stateManager.updateBlockB(newBlockB)
                    refreshState()
                }
            )
            
            // Block C - Calisthenics
            BlockCCard(
                blockC = state.blockC,
                onExerciseToggle = { exerciseId ->
                    stateManager.toggleExercise(BlockType.C, exerciseId)
                    refreshState()
                },
                onBlockComplete = { completed ->
                    val newBlockC = state.blockC.copy(isCompleted = completed)
                    stateManager.updateBlockC(newBlockC)
                    refreshState()
                }
            )
            
            // Block D - Mobility + Recovery
            BlockDCard(
                blockD = state.blockD,
                onExerciseToggle = { exerciseId ->
                    stateManager.toggleExercise(BlockType.D, exerciseId)
                    refreshState()
                },
                onWristRehabToggle = { enabled ->
                    val newBlockD = state.blockD.copy(wristRehabEnabled = enabled)
                    stateManager.updateBlockD(newBlockD)
                    refreshState()
                },
                onBlockComplete = { completed ->
                    val newBlockD = state.blockD.copy(isCompleted = completed)
                    stateManager.updateBlockD(newBlockD)
                    refreshState()
                }
            )
        }
    }
    
    // Reminder Settings Dialog
    if (showReminderSettings) {
        ReminderSettingsDialog(
            reminderTimes = state.reminderTimes,
            onDismiss = { showReminderSettings = false },
            onSave = { times ->
                stateManager.saveReminderTimes(times)
                reminderScheduler.rescheduleAllReminders()
                refreshState()
                showReminderSettings = false
            }
        )
    }
}

@Composable
fun DailyProgressCard(state: DailyTrainingState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.getCompletionPercentage().toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            LinearProgressIndicator(
                progress = { state.getCompletionPercentage() / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BlockStatusChip("A", state.blockA.isCompleted)
                BlockStatusChip("B", state.blockB.isCompleted)
                BlockStatusChip("C", state.blockC.isCompleted)
                BlockStatusChip("D", state.blockD.isCompleted)
            }
        }
    }
}

@Composable
fun BlockStatusChip(label: String, isCompleted: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isCompleted) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Block A - Cardio Base
@Composable
fun BlockACard(
    blockA: BlockA,
    onModeChange: (CardioMode) -> Unit,
    onSegmentComplete: (String) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    val option = when (blockA.selectedMode) {
        CardioMode.CYCLING -> BlockA.CYCLING_OPTION
        CardioMode.RUNNING -> BlockA.RUNNING_OPTION
        CardioMode.SWIMMING -> BlockA.SWIMMING_OPTION
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockA.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Block A - Cardio Base (15 min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Checkbox(
                    checked = blockA.isCompleted,
                    onCheckedChange = onBlockComplete
                )
            }
            
            // Mode selector
            Text("Select Cardio Mode:", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = blockA.selectedMode == CardioMode.CYCLING,
                    onClick = { onModeChange(CardioMode.CYCLING) },
                    label = { Text("Cycling") }
                )
                FilterChip(
                    selected = blockA.selectedMode == CardioMode.RUNNING,
                    onClick = { onModeChange(CardioMode.RUNNING) },
                    label = { Text("Running") }
                )
                FilterChip(
                    selected = blockA.selectedMode == CardioMode.SWIMMING,
                    onClick = { onModeChange(CardioMode.SWIMMING) },
                    label = { Text("Swimming") }
                )
            }
            
            // Segments with timers
            option.segments.forEach { segment ->
                SegmentRow(
                    segment = segment,
                    isCompleted = blockA.completedSegments.contains(segment.name),
                    onToggle = { onSegmentComplete(segment.name) }
                )
            }
        }
    }
}

@Composable
fun SegmentRow(
    segment: CardioSegment,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = isCompleted, onCheckedChange = { onToggle() })
            Text(
                text = "${segment.name} - ${segment.durationMinutes} min",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        ExerciseTimerButton(durationSeconds = segment.durationMinutes * 60)
    }
}

// Block B - Lower Body + Core
@Composable
fun BlockBCard(
    blockB: BlockB,
    onExerciseToggle: (String) -> Unit,
    onRestDurationChange: (Int) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockB.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Block B - Lower Body + Core (15 min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Checkbox(
                    checked = blockB.isCompleted,
                    onCheckedChange = onBlockComplete
                )
            }
            
            Text(
                text = "Round ${blockB.completedRounds + 1} of ${BlockB.TOTAL_ROUNDS}",
                style = MaterialTheme.typography.labelLarge
            )
            
            // Rest duration selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rest between rounds:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onRestDurationChange(30) },
                        enabled = blockB.restDurationSeconds != 30,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("30s")
                    }
                    Button(
                        onClick = { onRestDurationChange(45) },
                        enabled = blockB.restDurationSeconds != 45,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("45s")
                    }
                }
            }
            
            // Exercises
            BlockB.EXERCISES.forEach { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    isCompleted = blockB.completedExercises.contains(exercise.id),
                    onToggle = { onExerciseToggle(exercise.id) }
                )
            }
        }
    }
}

// Block C - Calisthenics
@Composable
fun BlockCCard(
    blockC: BlockC,
    onExerciseToggle: (String) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockC.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Block C - Calisthenics (15 min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Checkbox(
                    checked = blockC.isCompleted,
                    onCheckedChange = onBlockComplete
                )
            }
            
            Text(
                text = "Round ${blockC.completedRounds + 1} of ${BlockC.TOTAL_ROUNDS}",
                style = MaterialTheme.typography.labelLarge
            )
            
            BlockC.EXERCISES.forEach { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    isCompleted = blockC.completedExercises.contains(exercise.id),
                    onToggle = { onExerciseToggle(exercise.id) }
                )
            }
        }
    }
}

// Block D - Mobility + Recovery
@Composable
fun BlockDCard(
    blockD: BlockD,
    onExerciseToggle: (String) -> Unit,
    onWristRehabToggle: (Boolean) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockD.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Block D - Mobility + Recovery (15 min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Checkbox(
                    checked = blockD.isCompleted,
                    onCheckedChange = onBlockComplete
                )
            }
            
            Text("Mobility:", style = MaterialTheme.typography.labelLarge)
            BlockD.MOBILITY_EXERCISES.forEach { exercise ->
                ExerciseRow(
                    exercise = exercise,
                    isCompleted = blockD.completedMobility.contains(exercise.id),
                    onToggle = { onExerciseToggle(exercise.id) }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wrist Rehabilitation (if medically approved):", style = MaterialTheme.typography.labelMedium)
                Switch(
                    checked = blockD.wristRehabEnabled,
                    onCheckedChange = onWristRehabToggle
                )
            }
            
            if (blockD.wristRehabEnabled) {
                BlockD.WRIST_REHAB_EXERCISES.forEach { exercise ->
                    ExerciseRow(
                        exercise = exercise,
                        isCompleted = blockD.completedWristRehab.contains(exercise.id),
                        onToggle = { onExerciseToggle(exercise.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseRow(
    exercise: Exercise,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = isCompleted, onCheckedChange = { onToggle() })
            Text(
                text = exercise.getDisplayText(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (exercise.isTimed && exercise.durationSeconds != null) {
            ExerciseTimerButton(durationSeconds = exercise.durationSeconds)
        } else if (exercise.reps != null) {
            // Rep counter could be added here if needed
            Text(
                text = "${exercise.reps} reps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExerciseTimerButton(durationSeconds: Int) {
    var isRunning by remember { mutableStateOf(false) }
    var timeRemaining by remember { mutableStateOf(durationSeconds) }
    
    LaunchedEffect(isRunning) {
        while (isRunning && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
        }
        if (timeRemaining == 0) {
            isRunning = false
        }
    }
    
    Button(
        onClick = {
            if (isRunning) {
                isRunning = false
            } else {
                timeRemaining = durationSeconds
                isRunning = true
            }
        },
        modifier = Modifier.height(36.dp)
    ) {
        if (isRunning) {
            Text(formatTime(timeRemaining))
        } else {
            Text("Start ${formatTime(durationSeconds)}")
        }
    }
}

fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

// Reminder Settings Dialog
@Composable
fun ReminderSettingsDialog(
    reminderTimes: Map<BlockType, String>,
    onDismiss: () -> Unit,
    onSave: (Map<BlockType, String>) -> Unit
) {
    var times by remember { mutableStateOf(reminderTimes) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder Times") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BlockType.values().forEach { blockType ->
                    val blockName = when (blockType) {
                        BlockType.A -> "Block A"
                        BlockType.B -> "Block B"
                        BlockType.C -> "Block C"
                        BlockType.D -> "Block D"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(blockName)
                        TimePickerField(
                            time = times[blockType] ?: "08:00",
                            onTimeChange = { newTime ->
                                times = times + (blockType to newTime)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(times) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimePickerField(time: String, onTimeChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val (hour, minute) = time.split(":").let { 
        Pair(it[0].toIntOrNull() ?: 0, it[1].toIntOrNull() ?: 0) 
    }
    
    OutlinedButton(onClick = { showDialog = true }) {
        Text(time)
    }
    
    if (showDialog) {
        // Simple time input - in production, use a proper time picker
        var hourText by remember { mutableStateOf(hour.toString()) }
        var minuteText by remember { mutableStateOf(String.format("%02d", minute)) }
        
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Set Time") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { 
                            val h = it.toIntOrNull()
                            if (h != null && h in 0..23) {
                                hourText = it
                            }
                        },
                        label = { Text("Hour") },
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":")
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { 
                            val m = it.toIntOrNull()
                            if (m != null && m in 0..59) {
                                minuteText = String.format("%02d", m)
                            }
                        },
                        label = { Text("Minute") },
                        modifier = Modifier.width(80.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val h = hourText.toIntOrNull() ?: 0
                    val m = minuteText.toIntOrNull() ?: 0
                    onTimeChange(String.format("%02d:%02d", h, m))
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
