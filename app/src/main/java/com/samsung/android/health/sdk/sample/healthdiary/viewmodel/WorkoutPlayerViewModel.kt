package com.samsung.android.health.sdk.sample.healthdiary.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.samsung.android.health.sdk.sample.healthdiary.data.room.AppDatabase
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockSnapshot
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.WorkoutSessionEntity
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.RoutineBlockPayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.RoutinePayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.WorkoutAckPayload
import com.samsung.android.health.sdk.sample.healthdiary.workout.model.WorkoutEventPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.UUID

private const val TAG = "WorkoutPhonePlayer"
private const val WORKOUT_START_PATH = "/workout/start"
private const val WORKOUT_EVENT_PATH = "/workout/event"
private const val WORKOUT_ACK_PATH = "/workout/ack"
private const val WORKOUT_REP_PATH = "/workout/rep"

data class WorkoutPlayerUiState(
    val session: WorkoutSessionEntity? = null,
    val blocks: List<BlockSnapshot> = emptyList(),
    val completionState: Map<String, List<Boolean>> = emptyMap(),
    val liveReps: Map<String, Int> = emptyMap(), // blockId -> current rep count (for active block)
    val activeBlockId: String? = null,
    val activeSetIndex: Int? = null,
    val connectionStatus: String = "Connecting to watch...", // Connecting, Connected, Not confirmed
    val isFinished: Boolean = false
)

class WorkoutPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionDao = db.workoutSessionDao()
    private val routineDao = db.routineDao()
    private val messageClient = Wearable.getMessageClient(application)
    private val nodeClient = Wearable.getNodeClient(application)
    private val gson = Gson()
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(WorkoutPlayerUiState())
    val uiState: StateFlow<WorkoutPlayerUiState> = _uiState.asStateFlow()

    private val messageListener = MessageClient.OnMessageReceivedListener { event ->
        viewModelScope.launch {
            when (event.path) {
                WORKOUT_ACK_PATH -> {
                    try {
                        val payload = json.decodeFromString<WorkoutAckPayload>(String(event.data, Charsets.UTF_8))
                        if (payload.sessionId == _uiState.value.session?.sessionId) {
                             if (payload.status == "STARTED") {
                                 _uiState.value = _uiState.value.copy(connectionStatus = "Connected")
                             } else {
                                 _uiState.value = _uiState.value.copy(connectionStatus = "Watch rejected: ${payload.reason}")
                             }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse ACK", e)
                    }
                }
                WORKOUT_EVENT_PATH -> {
                    try {
                        val payload = json.decodeFromString<WorkoutEventPayload>(String(event.data, Charsets.UTF_8))
                        handleWatchEvent(payload)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse event", e)
                    }
                }
                WORKOUT_REP_PATH -> {
                    try {
                        val payload = json.decodeFromString<WorkoutEventPayload>(String(event.data, Charsets.UTF_8))
                        handleRepUpdate(payload)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse rep update", e)
                    }
                }
            }
        }
    }

    init {
        messageClient.addListener(messageListener)
        // Load active session if any
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.getActiveSession().collect { session ->
                if (session != null) {
                    loadSessionIntoState(session)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(messageListener)
    }

    fun startSession(routineId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val routine = routineDao.getRoutineByRoutineId(routineId) ?: return@launch
            val blocks = routineDao.getBlocksForRoutineSync(routineId)
            
            val sessionId = UUID.randomUUID().toString()
            val blockSnapshots = blocks.map { 
                BlockSnapshot(
                    blockId = it.blockId,
                    exerciseName = it.exerciseName,
                    sets = it.sets,
                    targetWeight = it.targetWeight,
                    targetReps = it.targetReps,
                    restSec = it.restSec,
                    orderIndex = it.orderIndex
                )
            }.sortedBy { it.orderIndex }

            // Init completion map: blockId -> List<Boolean> (all false)
            val completionMap = blockSnapshots.associate { block ->
                block.blockId to List(block.sets) { false }
            }

            val session = WorkoutSessionEntity(
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
            Log.d(TAG, "Session created sessionId=$sessionId")
            
            // Send to watch
            sendStartToWatch(session, blockSnapshots)
        }
    }

    fun joinSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.getSession(sessionId).collect { session ->
                if (session != null) {
                    loadSessionIntoState(session)
                    if (session.status == "RUNNING") {
                        _uiState.value = _uiState.value.copy(connectionStatus = "Connected")
                    }
                } else {
                    Log.w(TAG, "Session not found (yet) for join: $sessionId")
                    _uiState.value = _uiState.value.copy(connectionStatus = "Waiting for session data...")
                }
            }
        }
    }

    private suspend fun sendStartToWatch(session: WorkoutSessionEntity, blocks: List<BlockSnapshot>) {
        try {
            val nodes = nodeClient.connectedNodes.await()
            if (nodes.isEmpty()) {
                _uiState.value = _uiState.value.copy(connectionStatus = "Watch not connected")
                return
            }

            val payload = RoutinePayload(
                sessionId = session.sessionId,
                routineId = session.routineId,
                routineName = session.routineName,
                blocks = blocks.map { b ->
                    RoutineBlockPayload(
                        blockId = b.blockId,
                        exerciseName = b.exerciseName,
                        sets = b.sets,
                        targetWeight = b.targetWeight,
                        targetReps = b.targetReps,
                        restSec = b.restSec
                    )
                },
                sentAt = Instant.now().toString()
            )
            val jsonPayload = json.encodeToString(RoutinePayload.serializer(), payload)
            
            Log.i(TAG, "TX /workout/start sessionId=${session.sessionId}")
            nodes.forEach { node ->
                messageClient.sendMessage(node.id, WORKOUT_START_PATH, jsonPayload.toByteArray()).await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send start to watch", e)
            _uiState.value = _uiState.value.copy(connectionStatus = "Failed to send to watch")
        }
    }

    private fun loadSessionIntoState(session: WorkoutSessionEntity) {
        val typeBlock = object : TypeToken<List<BlockSnapshot>>() {}.type
        val blocks: List<BlockSnapshot> = gson.fromJson(session.blocksSnapshotJson, typeBlock)
        
        val typeCompletion = object : TypeToken<Map<String, List<Boolean>>>() {}.type
        val completion: Map<String, List<Boolean>> = gson.fromJson(session.completionStateJson, typeCompletion)

        _uiState.value = _uiState.value.copy(
            session = session,
            blocks = blocks,
            completionState = completion,
            isFinished = session.status == "FINISHED"
        )
    }

    fun toggleSet(blockId: String, setIndex: Int) {
        val currentSession = _uiState.value.session ?: return
        if (currentSession.status == "FINISHED") return

        val currentMap = _uiState.value.completionState.toMutableMap()
        val currentList = currentMap[blockId]?.toMutableList() ?: return

        if (setIndex < 0 || setIndex >= currentList.size) return

        // Toggle
        val newState = !currentList[setIndex]
        currentList[setIndex] = newState
        currentMap[blockId] = currentList

        // Update UI state immediately
        _uiState.value = _uiState.value.copy(completionState = currentMap)
        
        // Persist to database
        updateSessionState(currentSession, currentMap)

        Log.d(TAG, "Set toggled blockId=$blockId setIndex=$setIndex newState=$newState")

        // Send to watch
        if (newState) {
            sendEventToWatch(currentSession.sessionId, "DONE_SET", blockId, setIndex)
        } else {
            sendEventToWatch(currentSession.sessionId, "UNDO_SET", blockId, setIndex)
        }
    }
    
    private fun handleWatchEvent(payload: WorkoutEventPayload) {
        // Prevent loop
        if (payload.source == "PHONE") return

        when (payload.type) {
            "DONE_SET" -> {
                Log.i(TAG, "RX DONE_SET from WATCH sessionId=${payload.sessionId} blockId=${payload.blockId} setIndex=${payload.setIndex}")
                if (payload.blockId == null || payload.setIndex == null) return
                
                val currentSession = _uiState.value.session
                if (currentSession == null || currentSession.sessionId != payload.sessionId) return

                val currentMap = _uiState.value.completionState.toMutableMap()
                val currentList = currentMap[payload.blockId]?.toMutableList() ?: return
                
                if (payload.setIndex < 0 || payload.setIndex >= currentList.size) return
                
                // Idempotency: if already done, ignore
                if (currentList[payload.setIndex]) {
                    Log.d(TAG, "Ignoring duplicate DONE_SET")
                    return
                }

                currentList[payload.setIndex] = true
                currentMap[payload.blockId] = currentList
                
                // Update UI state immediately
                _uiState.value = _uiState.value.copy(completionState = currentMap)
                
                // Persist to database
                updateSessionState(currentSession, currentMap)
            }
            "UNDO_SET" -> {
                Log.i(TAG, "RX UNDO_SET from WATCH sessionId=${payload.sessionId} blockId=${payload.blockId} setIndex=${payload.setIndex}")
                if (payload.blockId == null || payload.setIndex == null) return
                
                val currentSession = _uiState.value.session
                if (currentSession == null || currentSession.sessionId != payload.sessionId) return

                val currentMap = _uiState.value.completionState.toMutableMap()
                val currentList = currentMap[payload.blockId]?.toMutableList() ?: return
                
                if (payload.setIndex < 0 || payload.setIndex >= currentList.size) return
                
                // Idempotency: if already undone, ignore
                if (!currentList[payload.setIndex]) {
                    return
                }

                currentList[payload.setIndex] = false
                currentMap[payload.blockId] = currentList
                
                // Update UI state immediately
                _uiState.value = _uiState.value.copy(completionState = currentMap)
                
                // Persist to database
                updateSessionState(currentSession, currentMap)
            }
            "FINISH_WORKOUT" -> {
                Log.i(TAG, "RX FINISH_WORKOUT from WATCH sessionId=${payload.sessionId}")
                finishWorkout(fromRemote = true)
            }
        }
    }
    
    private fun handleRepUpdate(payload: WorkoutEventPayload) {
        // Prevent loop
        if (payload.source == "PHONE") return
        
        if (payload.type != "REP_UPDATE") return
        if (payload.blockId == null || payload.setIndex == null || payload.repCount == null) return
        
        val currentSession = _uiState.value.session
        if (currentSession == null || currentSession.sessionId != payload.sessionId) return
        
        Log.i(TAG, "RX REP_UPDATE from WATCH blockId=${payload.blockId} setIndex=${payload.setIndex} reps=${payload.repCount}")
        
        // Update live reps state
        val currentReps = _uiState.value.liveReps.toMutableMap()
        currentReps[payload.blockId] = payload.repCount
        
        _uiState.value = _uiState.value.copy(
            liveReps = currentReps,
            activeBlockId = payload.blockId,
            activeSetIndex = payload.setIndex
        )
    }

    private fun updateSessionState(session: WorkoutSessionEntity, completionMap: Map<String, List<Boolean>>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedJson = gson.toJson(completionMap)
            val updatedSession = session.copy(completionStateJson = updatedJson)
            sessionDao.updateSession(updatedSession)
        }
    }
    
    private fun sendEventToWatch(sessionId: String, type: String, blockId: String?, setIndex: Int?) {
         viewModelScope.launch(Dispatchers.IO) {
             try {
                 val nodes = nodeClient.connectedNodes.await()
                 val payload = WorkoutEventPayload(
                     sessionId = sessionId,
                     blockId = blockId,
                     setIndex = setIndex,
                     type = type,
                     source = "PHONE",
                     at = Instant.now().toString()
                 )
                 val jsonPayload = json.encodeToString(WorkoutEventPayload.serializer(), payload)
                 Log.i("WorkoutSync", "TX $type sessionId=$sessionId blockId=$blockId set=$setIndex")
                 nodes.forEach { node ->
                     messageClient.sendMessage(node.id, WORKOUT_EVENT_PATH, jsonPayload.toByteArray()).await()
                 }
             } catch (e: Exception) {
                 Log.e(TAG, "Failed to send $type", e)
             }
         }
    }

    fun finishWorkout(fromRemote: Boolean = false) {
        val currentSession = _uiState.value.session ?: return
        viewModelScope.launch(Dispatchers.IO) {
            sessionDao.finishSession(currentSession.sessionId)
            _uiState.value = _uiState.value.copy(isFinished = true)
            
            if (!fromRemote) {
                sendEventToWatch(currentSession.sessionId, "FINISH_WORKOUT", null, null)
                Log.i("WorkoutSync", "TX FINISH_WORKOUT sessionId=${currentSession.sessionId}")
            }
        }
    }
}
