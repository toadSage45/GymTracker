package com.example.gymtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.db.ExerciseSet
import com.example.gymtracker.data.db.WorkoutSession
import com.example.gymtracker.data.program.GymProgram
import com.example.gymtracker.ui.theme.BluePrevious
import com.example.gymtracker.ui.theme.CardDark
import com.example.gymtracker.ui.theme.DarkSurfaceVariant
import com.example.gymtracker.ui.theme.GreenSuccess
import com.example.gymtracker.ui.theme.Orange40
import com.example.gymtracker.ui.theme.TextMuted
import com.example.gymtracker.ui.theme.TextSecondary
import com.example.gymtracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: WorkoutViewModel) {
    val state by viewModel.historyState.collectAsState()
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }

    if (state.isEditing && state.selectedSession != null) {
        EditSessionScreen(
            session = state.selectedSession!!,
            exercises = state.selectedExercises,
            programDayName = state.selectedProgramDay?.let {
                "Day ${it.dayNumber}: ${it.name}"
            } ?: "Unknown",
            onSetUpdate = { set, weight, reps -> viewModel.updateHistorySet(set, weight, reps) },
            onNotesUpdate = { viewModel.updateHistoryNotes(it) },
            onBack = { viewModel.closeHistoryEdit() }
        )
        return
    }

    if (sessionToDelete != null) {
        val session = sessionToDelete!!
        val programDay = GymProgram.getDayByNumber(session.dayNumber)
        val dayName = programDay?.let { "${it.name} (${it.focus})" } ?: "Day ${session.dayNumber}"
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Workout") },
            text = {
                Text("Delete $dayName on ${session.date} (Week ${GymProgram.absoluteWeekOf(session)})? This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHistorySession(session)
                    sessionToDelete = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    "Workout History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (state.sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workout history yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.sessions) { session ->
                    HistorySessionCard(
                        session = session,
                        onClick = { viewModel.selectHistorySession(session) },
                        onDelete = { sessionToDelete = session }
                    )
                }
            }
        }
    }
}

@Composable
fun HistorySessionCard(session: WorkoutSession, onClick: () -> Unit, onDelete: () -> Unit) {
    val programDay = GymProgram.getDayByNumber(session.dayNumber)
    val dayName = programDay?.let { "${it.name} (${it.focus})" } ?: "Day ${session.dayNumber}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (session.isCompleted) GreenSuccess.copy(alpha = 0.15f)
                        else Orange40.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (session.isCompleted) Icons.Default.Check else Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (session.isCompleted) GreenSuccess else Orange40,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Day ${session.dayNumber}: $dayName",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = session.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = "Week ${GymProgram.absoluteWeekOf(session)}" +
                            if (session.isCompleted) "" else " · not submitted",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSessionScreen(
    session: WorkoutSession,
    exercises: List<com.example.gymtracker.ui.viewmodel.ExerciseUiState>,
    programDayName: String,
    onSetUpdate: (ExerciseSet, Float?, Int?) -> Unit,
    onNotesUpdate: (String) -> Unit,
    onBack: () -> Unit
) {
    var showNotes by remember { mutableStateOf(false) }

    if (showNotes) {
        NotesDialog(
            currentNotes = session.notes,
            onSave = onNotesUpdate,
            onDismiss = { showNotes = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = programDayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${session.date} • Week ${GymProgram.absoluteWeekOf(session)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            },
            actions = {
                IconButton(onClick = { showNotes = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "Notes",
                        tint = TextSecondary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(exercises) { exerciseState ->
                EditExerciseCard(
                    exerciseState = exerciseState,
                    onWeightChange = { set, weight -> onSetUpdate(set, weight, set.reps) },
                    onRepsChange = { set, reps -> onSetUpdate(set, set.weight, reps) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun EditExerciseCard(
    exerciseState: com.example.gymtracker.ui.viewmodel.ExerciseUiState,
    onWeightChange: (ExerciseSet, Float?) -> Unit,
    onRepsChange: (ExerciseSet, Int?) -> Unit
) {
    val exercise = exerciseState.programExercise

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                color = Orange40,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Reps: ${exercise.repRange} • Rest: ${exercise.restTime}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (exerciseState.previousSets.isNotEmpty()) {
                PreviousWeekRow(exerciseState.previousSets, exerciseState.previousWeek)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!exercise.isTimedHold) {
                CurrentWeekRow(
                    sets = exerciseState.currentSets,
                    onWeightChange = onWeightChange,
                    onRepsChange = onRepsChange,
                    isCompleted = false
                )
            }
        }
    }
}
