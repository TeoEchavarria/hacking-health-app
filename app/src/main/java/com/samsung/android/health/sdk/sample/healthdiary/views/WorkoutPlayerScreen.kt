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
    onNavigateBack: () -> Unit,
    viewModel: WorkoutPlayerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(routineId) {
        if (routineId != null && uiState.session == null) {
            viewModel.startSession(routineId)
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
                Button(
                    onClick = { viewModel.finishWorkout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Finish Workout")
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
                    ExerciseBlockItem(
                        block = block,
                        completedSets = uiState.completionState[block.blockId] ?: emptyList(),
                        onSetToggle = { setIndex -> viewModel.toggleSet(block.blockId, setIndex) },
                        isSessionFinished = uiState.isFinished
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
    isSessionFinished: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = block.exerciseName,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${block.sets} sets • ${block.targetReps ?: "N/A"} reps • ${block.restSec}s rest",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
