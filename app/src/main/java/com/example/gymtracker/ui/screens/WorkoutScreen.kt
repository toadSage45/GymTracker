package com.example.gymtracker.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.db.ExerciseSet
import com.example.gymtracker.data.program.GymProgram
import com.example.gymtracker.data.program.ProgramExercise
import com.example.gymtracker.data.repository.DayStatus
import com.example.gymtracker.ui.theme.Amber
import com.example.gymtracker.ui.theme.BluePrevious
import com.example.gymtracker.ui.theme.CardDark
import com.example.gymtracker.ui.theme.DarkSurfaceVariant
import com.example.gymtracker.ui.theme.GreenSuccess
import com.example.gymtracker.ui.theme.Orange40
import com.example.gymtracker.ui.theme.TextMuted
import com.example.gymtracker.ui.theme.TextSecondary
import com.example.gymtracker.ui.viewmodel.ExerciseUiState
import com.example.gymtracker.ui.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(viewModel: WorkoutViewModel) {
    val state by viewModel.workoutState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Orange40)
        }
        return
    }

    val programDay = state.programDay ?: return
    val session = state.session ?: return

    if (state.showExerciseDetail != null) {
        ExerciseDetailSheet(
            exercise = state.showExerciseDetail!!,
            onDismiss = { viewModel.showExerciseDetail(null) }
        )
    }

    if (state.showNotes) {
        NotesDialog(
            currentNotes = session.notes,
            onSave = { viewModel.updateNotes(it) },
            onDismiss = { viewModel.toggleNotes() }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WeekDaySelector(
                week = state.selectedWeek,
                selectedDay = state.selectedDay,
                dayStatuses = state.dayStatuses,
                onWeekChange = { viewModel.selectWeek(it) },
                onDaySelect = { viewModel.selectDay(it) }
            )
        }

        item {
            WorkoutHeader(
                dayName = "Day ${programDay.dayNumber}: ${programDay.name}",
                focus = programDay.focus,
                week = state.selectedWeek,
                isSubmitted = session.isCompleted,
                date = session.date,
                onNotesClick = { viewModel.toggleNotes() }
            )
        }

        items(state.exercises) { exerciseState ->
            ExerciseCard(
                exerciseState = exerciseState,
                onWeightChange = { set, weight -> viewModel.updateSet(set, weight, set.reps) },
                onRepsChange = { set, reps -> viewModel.updateSet(set, set.weight, reps) },
                onComplete = { viewModel.toggleExerciseComplete(exerciseState.programExercise.id) },
                onInfoClick = { viewModel.showExerciseDetail(exerciseState.programExercise) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.toggleSubmitWorkout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (session.isCompleted) GreenSuccess.copy(alpha = 0.3f)
                    else if (state.allCompleted) GreenSuccess else Orange40
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (session.isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (session.isCompleted) "Undo Submit" else "Submit Workout",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun WeekDaySelector(
    week: Int,
    selectedDay: Int,
    dayStatuses: Map<Int, DayStatus>,
    onWeekChange: (Int) -> Unit,
    onDaySelect: (Int) -> Unit
) {
    val cycleWeek = GymProgram.cycleWeekOf(week)
    val block = GymProgram.blockOf(week)
    var showWeekPicker by remember { mutableStateOf(false) }

    if (showWeekPicker) {
        AlertDialog(
            onDismissRequest = { showWeekPicker = false },
            title = {
                Text(
                    "Select Week",
                    color = Orange40,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Explicitly select the program week:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val chunkedWeeks = (1..12).chunked(4)
                    chunkedWeeks.forEach { rowWeeks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowWeeks.forEach { w ->
                                val isCurrent = w == week
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isCurrent) Orange40 else DarkSurfaceVariant)
                                        .clickable {
                                            onWeekChange(w)
                                            showWeekPicker = false
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "W$w",
                                        color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWeekPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onWeekChange(week - 1) }, enabled = week > 1) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week",
                        tint = if (week > 1) Orange40 else TextMuted.copy(alpha = 0.3f)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showWeekPicker = true }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "WEEK $week",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Select week",
                            tint = Orange40,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = if (cycleWeek == GymProgram.WEEKS_PER_BLOCK) "Block $block · Deload (tap to change)"
                        else "Block $block · Week $cycleWeek of ${GymProgram.WEEKS_PER_BLOCK} (tap to change)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cycleWeek == GymProgram.WEEKS_PER_BLOCK) Amber else TextMuted
                    )
                }
                IconButton(onClick = { onWeekChange(week + 1) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next week",
                        tint = Orange40
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GymProgram.days.forEach { day ->
                    DayChip(
                        dayNumber = day.dayNumber,
                        name = day.name,
                        status = dayStatuses[day.dayNumber] ?: DayStatus.EMPTY,
                        isSelected = day.dayNumber == selectedDay,
                        onClick = { onDaySelect(day.dayNumber) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DayChip(
    dayNumber: Int,
    name: String,
    status: DayStatus,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        DayStatus.COMPLETED -> GreenSuccess
        DayStatus.IN_PROGRESS -> Amber
        DayStatus.EMPTY -> TextMuted.copy(alpha = 0.3f)
    }
    val background = when {
        isSelected -> Orange40
        status == DayStatus.COMPLETED -> GreenSuccess.copy(alpha = 0.12f)
        else -> DarkSurfaceVariant
    }
    val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(
                1.dp,
                if (isSelected) Orange40 else statusColor.copy(alpha = 0.5f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "D$dayNumber",
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White.copy(alpha = 0.85f) else TextMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (status == DayStatus.COMPLETED) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Submitted",
                tint = if (isSelected) Color.White else GreenSuccess,
                modifier = Modifier.size(12.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected && status == DayStatus.EMPTY) Color.White.copy(alpha = 0.4f) else statusColor)
            )
        }
    }
}

@Composable
fun WorkoutHeader(
    dayName: String,
    focus: String,
    week: Int,
    isSubmitted: Boolean,
    date: String,
    onNotesClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Orange40,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = focus,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onNotesClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = "Notes",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipLabel("Week $week")
                ChipLabel(if (isSubmitted) "Submitted $date" else "Not submitted")
            }
        }
    }
}

@Composable
fun ChipLabel(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
    }
}

@Composable
fun ExerciseCard(
    exerciseState: ExerciseUiState,
    onWeightChange: (ExerciseSet, Float?) -> Unit,
    onRepsChange: (ExerciseSet, Int?) -> Unit,
    onComplete: () -> Unit,
    onInfoClick: () -> Unit
) {
    val exercise = exerciseState.programExercise
    val borderColor by animateColorAsState(
        if (exerciseState.isCompleted) GreenSuccess.copy(alpha = 0.5f)
        else Color.Transparent,
        label = "border"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (exercise.isTimedHold) Icons.Default.Timer
                    else Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (exerciseState.isCompleted) GreenSuccess else Orange40,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Exercise details",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Reps: ${exercise.repRange}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Text(
                    text = "Rest: ${exercise.restTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (exerciseState.previousSets.isNotEmpty()) {
                PreviousWeekRow(exerciseState.previousSets, exerciseState.previousWeek)
                Spacer(modifier = Modifier.height(8.dp))
            } else if (!exercise.isTimedHold) {
                Text(
                    text = "No previous record — this is your baseline",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (!exercise.isTimedHold) {
                CurrentWeekRow(
                    sets = exerciseState.currentSets,
                    onWeightChange = onWeightChange,
                    onRepsChange = onRepsChange,
                    isCompleted = exerciseState.isCompleted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onComplete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (exerciseState.isCompleted) GreenSuccess.copy(alpha = 0.2f)
                    else DarkSurfaceVariant
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (exerciseState.isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                    contentDescription = null,
                    tint = if (exerciseState.isCompleted) GreenSuccess else TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (exerciseState.isCompleted) "Undo" else "Mark Complete",
                    color = if (exerciseState.isCompleted) GreenSuccess else TextSecondary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun PreviousWeekRow(sets: List<ExerciseSet>, fromWeek: Int? = null) {
    val weight = sets.firstOrNull()?.weight
    val repsStr = sets.joinToString("  ") { "${it.reps ?: "-"}" }
    val weightStr = when {
        weight == null -> "—"
        weight < 0 -> "${weight}kg"
        weight == 0f -> "BW"
        else -> "${formatWeight(weight)}kg"
    }

    Column {
        Text(
            text = if (fromWeek != null) "Previous · Week $fromWeek" else "Previous",
            style = MaterialTheme.typography.labelSmall,
            color = BluePrevious
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BluePrevious.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = weightStr,
                style = MaterialTheme.typography.bodyMedium,
                color = BluePrevious,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "→",
                color = BluePrevious.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = repsStr,
                style = MaterialTheme.typography.bodyMedium,
                color = BluePrevious
            )
        }
    }
}

@Composable
fun CurrentWeekRow(
    sets: List<ExerciseSet>,
    onWeightChange: (ExerciseSet, Float?) -> Unit,
    onRepsChange: (ExerciseSet, Int?) -> Unit,
    isCompleted: Boolean
) {
    val weightStr = sets.firstOrNull()?.weight?.let { formatWeight(it) } ?: ""

    Column {
        Text(
            text = "Current",
            style = MaterialTheme.typography.labelSmall,
            color = Orange40
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var weightInput by remember(weightStr) { mutableStateOf(weightStr) }
            OutlinedTextField(
                value = weightInput,
                onValueChange = { value ->
                    weightInput = value
                    val parsed = value.toFloatOrNull()
                    if (sets.isNotEmpty()) {
                        for (set in sets) {
                            onWeightChange(set, parsed)
                        }
                    }
                },
                label = { Text("kg", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.width(72.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = inputFieldColors(),
                enabled = !isCompleted,
                shape = RoundedCornerShape(10.dp)
            )

            Text("→", color = TextMuted, style = MaterialTheme.typography.bodySmall)

            sets.forEach { set ->
                var repsInput by remember(set.reps) {
                    mutableStateOf(set.reps?.toString() ?: "")
                }
                OutlinedTextField(
                    value = repsInput,
                    onValueChange = { value ->
                        repsInput = value
                        onRepsChange(set, value.toIntOrNull())
                    },
                    label = { Text("S${set.setNumber}", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.width(56.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = inputFieldColors(),
                    enabled = !isCompleted,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}

@Composable
fun inputFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Orange40,
    unfocusedBorderColor = TextMuted.copy(alpha = 0.3f),
    focusedLabelColor = Orange40,
    unfocusedLabelColor = TextMuted,
    cursorColor = Orange40,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = TextSecondary,
    disabledBorderColor = TextMuted.copy(alpha = 0.15f),
    disabledLabelColor = TextMuted.copy(alpha = 0.5f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailSheet(exercise: ProgramExercise, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Orange40.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (exercise.isTimedHold) Icons.Default.Timer
                        else Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = Orange40,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            DetailRow("Rep Range", exercise.repRange)
            DetailRow("Rest Time", exercise.restTime)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Execution Cues",
                style = MaterialTheme.typography.titleSmall,
                color = Orange40,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.executionCues,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sets per Week",
                style = MaterialTheme.typography.titleSmall,
                color = Orange40,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                exercise.setsPerWeek.forEachIndexed { index, sets ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (index == 5) "DL" else "W${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = "$sets",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun NotesDialog(currentNotes: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var notes by remember { mutableStateOf(currentNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Day Notes", color = Orange40, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Add notes for this workout...") },
                colors = inputFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(notes)
                onDismiss()
            }) {
                Text("Save", color = Orange40)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardDark,
        shape = RoundedCornerShape(16.dp)
    )
}

fun formatWeight(weight: Float): String {
    return if (weight == weight.toInt().toFloat()) {
        weight.toInt().toString()
    } else {
        weight.toString()
    }
}
