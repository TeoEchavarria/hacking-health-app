package com.samsung.android.health.sdk.sample.healthdiary.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samsung.android.health.sdk.sample.healthdiary.components.SandboxTopBar
import com.samsung.android.health.sdk.sample.healthdiary.viewmodel.WorkoutPlayerViewModel
import com.samsung.android.health.sdk.sample.healthdiary.workout.data.BlockSnapshot

@Composable
fun WorkoutPlayerScreen(
    routineId: String?,
    sessionId: String? = null,
    onNavigateBack: () -> Unit,
    onFinishSession: () -> Unit = {},
    viewModel: WorkoutPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(routineId, sessionId) {
        if (uiState.session == null) {
            if (sessionId != null) {
                viewModel.joinSession(sessionId)
            } else if (routineId != null) {
                viewModel.startSession(routineId)
            }
        }
    }

    Scaffold(
        topBar = {
            SandboxTopBar(
                title = uiState.session?.routineName ?: "Workout",
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            if (!uiState.isFinished && uiState.session != null) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            viewModel.finishWorkout()
                            onFinishSession()
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("End Workout")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Status Banner
            if (uiState.connectionStatus.isNotEmpty() && !uiState.isFinished) {
                Surface(
                    color = if (uiState.connectionStatus == "Connected") 
                        MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.connectionStatus,
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            if (uiState.isFinished) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Workout Finished",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.blocks) { block ->
                    val isActiveBlock = block.blockId == uiState.activeBlockId
                    val liveReps = if (isActiveBlock) uiState.liveReps[block.blockId] else null
                    ExerciseBlockItem(
                        block = block,
                        completedSets = uiState.completionState[block.blockId] ?: emptyList(),
                        onSetToggle = { setIndex -> viewModel.toggleSet(block.blockId, setIndex) },
                        isSessionFinished = uiState.isFinished,
                        isActive = isActiveBlock,
                        liveReps = liveReps
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseBlockItem(
    block: BlockSnapshot,
    completedSets: List<Boolean>,
    onSetToggle: (Int) -> Unit,
    isSessionFinished: Boolean,
    isActive: Boolean = false,
    liveReps: Int? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 6.dp else 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                // Live rep counter for active block
                if (isActive && liveReps != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (block.targetReps != null && block.targetReps > 0) {
                                "$liveReps/${block.targetReps}"
                            } else {
                                "$liveReps reps"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${block.sets} sets • ${block.targetReps ?: "N/A"} reps • ${block.restSec}s rest",
                style = MaterialTheme.typography.bodySmall,
                color = if (isActive) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (block.targetWeight > 0) {
                Text(
                    text = "${block.targetWeight} kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 0 until block.sets) {
                    val isCompleted = completedSets.getOrElse(i) { false }
                    SetCircle(
                        index = i + 1,
                        isCompleted = isCompleted,
                        enabled = !isSessionFinished,
                        onClick = { onSetToggle(i) }
                    )
                }
            }
        }
    }
}

@Composable
fun SetCircle(
    index: Int,
    isCompleted: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isCompleted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isCompleted) Color.Transparent else MaterialTheme.colorScheme.outline
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Set $index done",
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = index.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}
