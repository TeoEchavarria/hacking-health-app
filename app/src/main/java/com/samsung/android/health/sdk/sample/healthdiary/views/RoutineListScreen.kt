package com.samsung.android.health.sdk.sample.healthdiary.views

import android.util.Log
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
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.RoutineEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.RoutineBlockPayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.RoutinePayload
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

private const val WORKOUT_PROTOCOL_PHONE_TAG = "WorkoutProtocolPhone"
private const val WORKOUT_START_PATH = "/workout/start"
private const val WORKOUT_ACK_PATH = "/workout/ack"
private const val ACK_TIMEOUT_MS = 2000L

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String?) -> Unit
) {
    val context = LocalContext.current
    val routineDao = remember { AppDatabase.getDatabase(context).routineDao() }
    val routines by routineDao.getAllRoutines().collectAsState(initial = emptyList())
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

    val ackReceivedFlow = remember { MutableSharedFlow<Pair<String, String?>>() }
    val pendingAckSessionId = remember { MutableStateFlow<String?>(null) }

    DisposableEffect(context) {
        val messageClient = Wearable.getMessageClient(context)
        val listener = MessageClient.OnMessageReceivedListener { event ->
            if (event.path != WORKOUT_ACK_PATH) return@OnMessageReceivedListener
            try {
                val payload = json.decodeFromString<AckPayload>(String(event.data, Charsets.UTF_8))
                val sid = payload.sessionId
                val status = payload.status
                if (sid != null && status != null && sid == pendingAckSessionId.value) {
                    Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "RX /workout/ack status=$status sessionId=$sid")
                    pendingAckSessionId.value = null
                    ackReceivedFlow.tryEmit(status to payload.reason)
                }
            } catch (e: Exception) {
                Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Failed to parse workout ack", e)
            }
        }
        messageClient.addListener(listener)
        onDispose { messageClient.removeListener(listener) }
    }

    var startingRoutineId by remember { mutableStateOf<String?>(null) }
    var routineToDelete by remember { mutableStateOf<RoutineEntity?>(null) }

    fun deleteRoutine(routine: RoutineEntity) {
        scope.launch {
            routineDao.deleteRoutineAndBlocks(routine)
            routineToDelete = null
            snackbarHostState.showSnackbar(message = "Routine deleted", duration = SnackbarDuration.Short)
        }
    }

    fun startRoutine(routine: RoutineEntity) {
        scope.launch {
            startingRoutineId = routine.routineId
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            if (nodes.isEmpty()) {
                startingRoutineId = null
                snackbarHostState.showSnackbar(
                    message = "No watch connected. Make sure Bluetooth is on and the watch is paired.",
                    duration = SnackbarDuration.Long
                )
                return@launch
            }
            val blocks = routineDao.getBlocksForRoutineSync(routine.routineId)
            if (blocks.isEmpty()) {
                startingRoutineId = null
                snackbarHostState.showSnackbar(message = "Routine has no exercises. Add blocks in the editor.", duration = SnackbarDuration.Short)
                return@launch
            }
            val sessionId = UUID.randomUUID().toString()
            val blockPayloads = blocks.map { b ->
                RoutineBlockPayload(
                    blockId = b.blockId,
                    exerciseName = b.exerciseName,
                    sets = b.sets,
                    targetWeight = b.targetWeight,
                    targetReps = b.targetReps,
                    restSec = b.restSec
                )
            }
            val payload = RoutinePayload(
                sessionId = sessionId,
                routineId = routine.routineId,
                routineName = routine.name,
                blocks = blockPayloads,
                sentAt = Instant.now().toString()
            )
            val jsonPayload = json.encodeToString(RoutinePayload.serializer(), payload)
            pendingAckSessionId.value = sessionId
            Log.i(WORKOUT_PROTOCOL_PHONE_TAG, "TX /workout/start sessionId=$sessionId")
            var sendOk = false
            for (node in nodes) {
                try {
                    Wearable.getMessageClient(context).sendMessage(node.id, WORKOUT_START_PATH, jsonPayload.toByteArray(Charsets.UTF_8)).await()
                    sendOk = true
                } catch (e: Exception) {
                    Log.e(WORKOUT_PROTOCOL_PHONE_TAG, "Send failed to node ${node.displayName}", e)
                }
            }
            if (!sendOk) {
                startingRoutineId = null
                pendingAckSessionId.value = null
                snackbarHostState.showSnackbar(message = "Failed to send to watch.", duration = SnackbarDuration.Short)
                return@launch
            }
            val ackResult = withTimeoutOrNull(ACK_TIMEOUT_MS) { ackReceivedFlow.first() }
            startingRoutineId = null
            if (ackResult != null) {
                val (status, reason) = ackResult
                when (status) {
                    "STARTED" -> snackbarHostState.showSnackbar(message = "Running on watch", duration = SnackbarDuration.Short)
                    "REJECTED" -> snackbarHostState.showSnackbar(
                        message = reason?.takeIf { it.isNotBlank() } ?: "Watch rejected the workout.",
                        duration = SnackbarDuration.Long
                    )
                    else -> snackbarHostState.showSnackbar(message = "Unexpected response", duration = SnackbarDuration.Short)
                }
            } else {
                pendingAckSessionId.value = null
                Log.w(WORKOUT_PROTOCOL_PHONE_TAG, "ACK timeout sessionId=$sessionId")
                snackbarHostState.showSnackbar(
                    message = "Workout started on watch, open the watch screen.",
                    duration = SnackbarDuration.Long
                )
            }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        onClick = { startRoutine(routine) },
                        enabled = startingRoutineId == null || startingRoutineId == routine.routineId,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (startingRoutineId == routine.routineId) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(if (startingRoutineId == routine.routineId) "Starting…" else "Start")
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

@kotlinx.serialization.Serializable
private data class AckPayload(
    @kotlinx.serialization.SerialName("sessionId") val sessionId: String? = null,
    @kotlinx.serialization.SerialName("routineId") val routineId: String? = null,
    @kotlinx.serialization.SerialName("status") val status: String? = null,
    @kotlinx.serialization.SerialName("reason") val reason: String? = null,
    @kotlinx.serialization.SerialName("at") val at: String? = null
)
