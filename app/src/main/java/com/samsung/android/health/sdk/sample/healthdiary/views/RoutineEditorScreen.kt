package com.samsung.android.health.sdk.sample.healthdiary.views

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorScreen(
    routineId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val routineDao = remember { AppDatabase.getDatabase(context).routineDao() }
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var blocks by remember { mutableStateOf<List<BlockEntity>>(emptyList()) }
    var showBlockDialog by remember { mutableStateOf<BlockEntity?>(null) }
    var effectiveRoutineId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val isNew = routineId == null || routineId == "new"

    LaunchedEffect(routineId) {
        if (!isNew && routineId != null) {
            effectiveRoutineId = routineId
            val routine = routineDao.getRoutineByRoutineId(routineId)
            routine?.let {
                name = it.name
                blocks = routineDao.getBlocksForRoutineSync(routineId)
            }
        } else {
            effectiveRoutineId = UUID.randomUUID().toString()
            name = "New Routine"
            blocks = emptyList()
        }
    }

    fun saveRoutine() {
        scope.launch {
            val rid = effectiveRoutineId ?: UUID.randomUUID().toString()
            val routineEntity = RoutineEntity(routineId = rid, name = name)
            val blocksWithOrder = blocks.mapIndexed { i, b -> b.copy(routineId = rid, orderIndex = i) }
            Log.i("RoutineEditor", "Saving routine $name with ${blocks.size} exercises")
            routineDao.saveRoutineWithBlocks(routineEntity, blocksWithOrder)
            Log.i("RoutineEditor", "Routine saved successfully")
            onNavigateBack()
        }
    }

    fun addBlock() {
        val bid = UUID.randomUUID().toString()
        val rid = effectiveRoutineId ?: run {
            val id = UUID.randomUUID().toString()
            effectiveRoutineId = id
            id
        }
        showBlockDialog = BlockEntity(
            blockId = bid,
            routineId = rid,
            exerciseName = "Exercise",
            sets = 4,
            targetWeight = 0f,
            targetReps = null,
            restSec = 60,
            orderIndex = blocks.size
        )
    }

    fun editBlock(block: BlockEntity) {
        showBlockDialog = block
    }

    fun removeBlock(block: BlockEntity) {
        blocks = blocks.filter { it.blockId != block.blockId }
    }

    fun deleteRoutine() {
        scope.launch {
            val rid = effectiveRoutineId ?: return@launch
            val routine = routineDao.getRoutineByRoutineId(rid) ?: return@launch
            routineDao.deleteRoutineAndBlocks(routine)
            showDeleteConfirm = false
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            SandboxTopBar(
                title = if (isNew) "New Routine" else "Edit Routine",
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
                value = name,
                onValueChange = { name = it },
                label = { Text("Routine name") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Exercises", style = MaterialTheme.typography.titleMedium)
            blocks.forEach { block ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(block.exerciseName, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "${block.sets} sets · ${block.targetWeight.toInt()} kg" +
                                (block.targetReps?.let { " · $it reps" } ?: "") +
                                " · ${block.restSec}s rest",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row {
                        TextButton(onClick = { editBlock(block) }) { Text("Edit") }
                        IconButton(onClick = { removeBlock(block) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = { addBlock() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add exercise")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { saveRoutine() }, modifier = Modifier.fillMaxWidth()) {
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
                    Text("Delete routine", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete routine") },
            text = { Text("Delete \"$name\"? All exercises will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { deleteRoutine() }) {
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

    showBlockDialog?.let { block ->
        BlockEditDialog(
            block = block,
            onDismiss = { showBlockDialog = null },
            onSave = { updated ->
                blocks = blocks.map { if (it.blockId == updated.blockId) updated else it }
                    .let { list -> if (list.any { it.blockId == updated.blockId }) list else list + updated }
                showBlockDialog = null
            }
        )
    }
}

@Composable
private fun BlockEditDialog(
    block: BlockEntity,
    onDismiss: () -> Unit,
    onSave: (BlockEntity) -> Unit
) {
    var exerciseName by remember(block) { mutableStateOf(block.exerciseName) }
    var sets by remember(block) { mutableStateOf(block.sets.toString()) }
    var targetWeight by remember(block) { mutableStateOf(block.targetWeight.toString()) }
    var targetReps by remember(block) { mutableStateOf(block.targetReps?.toString() ?: "") }
    var restSec by remember(block) { mutableStateOf(block.restSec.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = exerciseName,
                    onValueChange = { exerciseName = it },
                    label = { Text("Exercise name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    label = { Text("Target weight (kg)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetReps,
                    onValueChange = { targetReps = it },
                    label = { Text("Target reps (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = restSec,
                    onValueChange = { restSec = it },
                    label = { Text("Rest (seconds)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(block.copy(
                    exerciseName = exerciseName,
                    sets = sets.toIntOrNull() ?: 4,
                    targetWeight = targetWeight.toFloatOrNull() ?: 0f,
                    targetReps = targetReps.toIntOrNull(),
                    restSec = restSec.toIntOrNull() ?: 60
                ))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
