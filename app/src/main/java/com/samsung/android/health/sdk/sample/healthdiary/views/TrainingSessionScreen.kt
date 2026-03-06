package com.samsung.android.health.sdk.sample.healthdiary.views

import android.Manifest
import android.util.Log
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
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
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.components.*
import com.samsung.android.health.sdk.sample.healthdiary.training.*
import com.samsung.android.health.sdk.sample.healthdiary.wearable.WorkoutAckMessage
import com.samsung.android.health.sdk.sample.healthdiary.wearable.WorkoutProtocol
import com.samsung.android.health.sdk.sample.healthdiary.wearable.WorkoutStartMessage
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.WorkoutEventPayload
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.*

private const val WORKOUT_PROTOCOL_PHONE_TAG = "WorkoutProtocolPhone"
private const val WEAR_CONN_TAG = "WearConn"
private const val WEAR_HANDSHAKE_TAG = "WearHandshake"
private const val ACK_TIMEOUT_MS = 3000L
private val BACKOFF_MS = listOf(500L, 1000L, 2000L)

private enum class WorkoutStartState { Idle, Connecting, Started, Error }

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSessionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlayer: (String) -> Unit // sessionId
) {
    val context = LocalContext.current
    val stateManager = remember { TrainingStateManager(context) }
    val reminderScheduler = remember { TrainingReminderScheduler(context) }
    val scope = rememberCoroutineScope()
    
    var state by remember { mutableStateOf(stateManager.getTodayState()) }
    var showReminderSettings by remember { mutableStateOf(false) }
    
    // Protocol v2: ACK by attemptId
    var workoutStartState by remember { mutableStateOf(WorkoutStartState.Idle) }
    val snackbarHostState = remember { SnackbarHostState() }
    val ackReceivedFlow = remember { MutableSharedFlow<WorkoutAckMessage>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST) }

    // Register MessageClient listener for /workout/ack (V2 WorkoutAckMessage)
    DisposableEffect(context) {
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event: MessageEvent ->
            if (event.path != WorkoutProtocol.PATH_ACK) return@OnMessageReceivedListener
            try {
                val ack = json.decodeFromString<WorkoutAckMessage>(String(event.data, Charsets.UTF_8))
                Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "event=ack_rx attemptId=${ack.attemptId} success=${ack.success} reasonCode=${ack.reasonCode}")
                ackReceivedFlow.tryEmit(ack)
            } catch (e: Exception) {
                Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Failed to parse workout ack V2", e)
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
        // Seed daily routines in Room (so WorkoutPlayer can load them)
        com.samsung.android.health.sdk.sample.healthdiary.workout.data.DailyRoutinesSeeder.seed(context)

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

    /** Protocol v2: map block type to routineId and display name for watch repository. */
    fun getRoutineIdAndName(blockType: BlockType): Pair<String, String> = when (blockType) {
        BlockType.A -> {
            val id = when (state.blockA.selectedMode) {
                CardioMode.CYCLING -> "daily_block_a_cycling"
                CardioMode.RUNNING -> "daily_block_a_running"
                CardioMode.SWIMMING -> "daily_block_a_swimming"
            }
            id to "Block A - Cardio Base"
        }
        BlockType.B -> {
            val rest = state.blockB.restDurationSeconds
            val id = if (rest == 45) "daily_block_b_45" else "daily_block_b_30"
            id to "Block B - Lower Body + Core"
        }
        BlockType.C -> "daily_block_c" to "Block C - Calisthenics"
        BlockType.D -> "daily_block_d" to "Block D - Mobility + Recovery"
    }

    fun startBlockWorkout(blockType: BlockType) {
        scope.launch {
            if (state.activeBlock != null) {
                snackbarHostState.showSnackbar("Please finish or cancel the current block first", duration = SnackbarDuration.Short)
                return@launch
            }

            workoutStartState = WorkoutStartState.Connecting
            val nodeClient = Wearable.getNodeClient(context)
            val nodes = nodeClient.connectedNodes.await()
            Log.i(WEAR_CONN_TAG, "event=node_discovery result=${nodes.size} nodeIds=${nodes.map { it.id.take(8) }}")

            if (nodes.isEmpty()) {
                workoutStartState = WorkoutStartState.Error
                Log.w(WORKOUT_PROTOCOL_PHONE_TAG, "event=no_nodes result=Watch not reachable")
                snackbarHostState.showSnackbar("Watch not reachable", duration = SnackbarDuration.Long)
                return@launch
            }

            val (routineId, routineName) = getRoutineIdAndName(blockType)
            val sessionId = UUID.randomUUID().toString()
            val messageClient = Wearable.getMessageClient(context)
            val startTime = System.currentTimeMillis()

            for (attempt in 0..2) {
                val attemptId = UUID.randomUUID().toString()
                val sentAt = System.currentTimeMillis()
                val msg = WorkoutStartMessage(
                    protocolVersion = WorkoutProtocol.PROTOCOL_VERSION,
                    type = WorkoutProtocol.TYPE_WORKOUT_START,
                    sessionId = sessionId,
                    attemptId = attemptId,
                    routineId = routineId,
                    routineName = routineName,
                    blockId = null,
                    sentAt = sentAt
                )
                val payloadBytes = json.encodeToString(WorkoutStartMessage.serializer(), msg).toByteArray(Charsets.UTF_8)
                Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "event=send_attempt attemptId=$attemptId sessionId=$sessionId routineId=$routineId blockId=null retry=$attempt timeoutMs=$ACK_TIMEOUT_MS payloadSize=${payloadBytes.size}")

                var sendOk = false
                for (node in nodes) {
                    try {
                        messageClient.sendMessage(node.id, WorkoutProtocol.PATH_START, payloadBytes).await()
                        sendOk = true
                        Log.d(WEAR_CONN_TAG, "event=send_ok node=${node.displayName}")
                        break
                    } catch (e: Exception) {
                        Log.e(WEAR_CONN_TAG, "event=send_failed node=${node.displayName}", e)
                    }
                }
                if (!sendOk) {
                    Log.w(WORKOUT_PROTOCOL_PHONE_TAG, "event=send_failed attemptId=$attemptId result=${WorkoutProtocol.ReasonCode.SEND_FAILED}")
                    if (attempt < 2) delay(BACKOFF_MS[attempt])
                    continue
                }

                val ack = withTimeoutOrNull(ACK_TIMEOUT_MS) { 
                    ackReceivedFlow.filter { it.attemptId == attemptId }.first() 
                }
                val elapsedMs = System.currentTimeMillis() - startTime
                if (ack != null) {
                    Log.i(WEAR_HANDSHAKE_TAG, "event=ack_received attemptId=$attemptId sessionId=$sessionId routineId=$routineId retry=$attempt result=${ack.success} reason=${ack.reasonCode} elapsedMs=$elapsedMs")
                    if (ack.success) {
                        stateManager.setActiveBlock(blockType)
                        stateManager.setActiveWorkoutSession(sessionId)
                        refreshState()
                        workoutStartState = WorkoutStartState.Started

                        // Create session in Room and navigate to player
                        val db = com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase.getDatabase(context)
                        val routineDao = db.routineDao()
                        val sessionDao = db.workoutSessionDao()
                        val gson = com.google.gson.Gson()

                        val routine = routineDao.getRoutineByRoutineId(routineId)
                        if (routine != null) {
                            val blocks = routineDao.getBlocksForRoutineSync(routineId)
                            val blockSnapshots = blocks.map {
                                com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockSnapshot(
                                    blockId = it.blockId,
                                    exerciseName = it.exerciseName,
                                    sets = it.sets,
                                    targetWeight = it.targetWeight,
                                    targetReps = it.targetReps,
                                    restSec = it.restSec,
                                    orderIndex = it.orderIndex
                                )
                            }.sortedBy { it.orderIndex }

                            val completionMap = blockSnapshots.associate { block ->
                                block.blockId to List(block.sets) { false }
                            }

                            val session = com.samsung.android.health.sdk.sample.healthdiary.workout.data.WorkoutSessionEntity(
                                sessionId = sessionId,
                                routineId = routineId,
                                routineName = routine.name,
                                startedAt = System.currentTimeMillis(),
                                status = "RUNNING",
                                blocksSnapshotJson = gson.toJson(blockSnapshots),
                                completionStateJson = gson.toJson(completionMap),
                                activeBlockIndex = 0,
                                activeSetIndex = 0
                            )
                            sessionDao.insertSession(session)
                            onNavigateToPlayer(sessionId)
                        } else {
                            Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Routine not found in Room: $routineId. Seeding issue?")
                            snackbarHostState.showSnackbar("Error: Routine definition not found. Try restarting app.", duration = SnackbarDuration.Long)
                        }

                        return@launch
                    }
                    // Watch rejected: do not retry
                    workoutStartState = WorkoutStartState.Error
                    val reasonCode = ack.reasonCode ?: "REJECTED"
                    
                    if (reasonCode == "SESSION_ALREADY_ACTIVE") {
                        val activeId = ack.reasonMessage
                        val result = snackbarHostState.showSnackbar(
                            message = "Watch has active session. Stop it?",
                            actionLabel = "Stop",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed && activeId != null) {
                            // Force stop the remote session
                            try {
                                val payload = WorkoutEventPayload(
                                    sessionId = activeId,
                                    type = "FINISH_WORKOUT",
                                    source = "PHONE",
                                    at = Instant.now().toString(),
                                    blockId = null,
                                    setIndex = null
                                )
                                val jsonPayload = json.encodeToString(WorkoutEventPayload.serializer(), payload)
                                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                                nodes.forEach { node ->
                                    Wearable.getMessageClient(context).sendMessage(node.id, WorkoutProtocol.PATH_EVENT, jsonPayload.toByteArray()).await()
                                }
                                Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "Sent FINISH_WORKOUT for stale session $activeId")
                            } catch (e: Exception) {
                                Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Failed to stop remote session", e)
                            }
                        }
                    } else {
                        snackbarHostState.showSnackbar("Watch rejected request: $reasonCode", duration = SnackbarDuration.Long)
                    }
                    
                    Log.w(WORKOUT_PROTOCOL_PHONE_TAG, "event=watch_rejected attemptId=$attemptId reasonCode=$reasonCode reasonMessage=${ack.reasonMessage}")
                    return@launch
                }
                Log.w(WORKOUT_PROTOCOL_PHONE_TAG, "event=ack_timeout attemptId=$attemptId retry=$attempt elapsedMs=$elapsedMs result=${WorkoutProtocol.ReasonCode.ACK_TIMEOUT}")
                if (attempt < 2) delay(BACKOFF_MS[attempt])
            }

            workoutStartState = WorkoutStartState.Error
            val elapsedMs = System.currentTimeMillis() - startTime
            Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "event=failed_after_retries sessionId=$sessionId routineId=$routineId result=${WorkoutProtocol.ReasonCode.ACK_TIMEOUT} elapsedMs=$elapsedMs")
            snackbarHostState.showSnackbar("Watch did not respond", duration = SnackbarDuration.Short)
        }
    }
    
    fun cancelActiveBlock() {
        val activeSessionId = state.activeSessionId
        scope.launch {
            if (activeSessionId != null) {
                try {
                    val messageClient = Wearable.getMessageClient(context)
                    val nodeClient = Wearable.getNodeClient(context)
                    val nodes = nodeClient.connectedNodes.await()
                    
                    val payload = WorkoutEventPayload(
                        sessionId = activeSessionId,
                        type = "FINISH_WORKOUT",
                        source = "PHONE",
                        at = Instant.now().toString(),
                        blockId = null,
                        setIndex = null
                    )
                    val jsonPayload = json.encodeToString(WorkoutEventPayload.serializer(), payload)
                    
                    nodes.forEach { node ->
                        messageClient.sendMessage(node.id, WorkoutProtocol.PATH_EVENT, jsonPayload.toByteArray()).await()
                    }
                    
                    val db = com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase.getDatabase(context)
                    db.workoutSessionDao().finishSession(activeSessionId)
                } catch (e: Exception) {
                    Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Failed to cancel workout", e)
                }
            }
            stateManager.setActiveBlock(null)
            stateManager.setActiveWorkoutSession(null)
            refreshState()
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
            
            // Cancel active block button
            if (state.activeBlock != null) {
                Button(
                    onClick = { cancelActiveBlock() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Stop Active Workout")
                }
            }
            
            // Block A - Cardio Base
            BlockACard(
                blockA = state.blockA,
                isActive = state.activeBlock == BlockType.A,
                onStart = { startBlockWorkout(BlockType.A) },
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
                isActive = state.activeBlock == BlockType.B,
                onStart = { startBlockWorkout(BlockType.B) },
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
                isActive = state.activeBlock == BlockType.C,
                onStart = { startBlockWorkout(BlockType.C) },
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
                isActive = state.activeBlock == BlockType.D,
                onStart = { startBlockWorkout(BlockType.D) },
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
    isActive: Boolean,
    onStart: () -> Unit,
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
            } else if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                if (isActive) {
                    Text("ACTIVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                } else if (!blockA.isCompleted) {
                    Button(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start Block")
                    }
                } else {
                    Checkbox(
                        checked = blockA.isCompleted,
                        onCheckedChange = onBlockComplete
                    )
                }
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
    isActive: Boolean,
    onStart: () -> Unit,
    onExerciseToggle: (String) -> Unit,
    onRestDurationChange: (Int) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockB.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                if (isActive) {
                    Text("ACTIVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                } else if (!blockB.isCompleted) {
                    Button(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start Block")
                    }
                } else {
                    Checkbox(
                        checked = blockB.isCompleted,
                        onCheckedChange = onBlockComplete
                    )
                }
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
    isActive: Boolean,
    onStart: () -> Unit,
    onExerciseToggle: (String) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockC.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                if (isActive) {
                    Text("ACTIVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                } else if (!blockC.isCompleted) {
                    Button(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start Block")
                    }
                } else {
                    Checkbox(
                        checked = blockC.isCompleted,
                        onCheckedChange = onBlockComplete
                    )
                }
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
    isActive: Boolean,
    onStart: () -> Unit,
    onExerciseToggle: (String) -> Unit,
    onWristRehabToggle: (Boolean) -> Unit,
    onBlockComplete: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (blockD.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
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
                if (isActive) {
                    Text("ACTIVE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                } else if (!blockD.isCompleted) {
                    Button(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Start Block")
                    }
                } else {
                    Checkbox(
                        checked = blockD.isCompleted,
                        onCheckedChange = onBlockComplete
                    )
                }
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
