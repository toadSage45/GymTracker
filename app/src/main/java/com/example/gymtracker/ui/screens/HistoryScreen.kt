package com.example.gymtracker.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymtracker.data.db.ExerciseSet
import com.example.gymtracker.data.db.WorkoutSession
import com.example.gymtracker.data.program.GymProgram
import com.example.gymtracker.ui.theme.Amber
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
    var selectedTab by remember { mutableStateOf(0) } // 0 = Log, 1 = Analytics

    BackHandler(enabled = state.isEditing && state.selectedSession != null) {
        viewModel.closeHistoryEdit()
    }

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
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardDark,
            contentColor = Orange40,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Orange40
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("History Log")
                    }
                },
                selectedContentColor = Orange40,
                unselectedContentColor = TextMuted
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Analytics")
                    }
                },
                selectedContentColor = Orange40,
                unselectedContentColor = TextMuted
            )
        }

        if (selectedTab == 0) {
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
                    items(state.sessions, key = { it.id }) { session ->
                        HistorySessionCard(
                            session = session,
                            onClick = { viewModel.selectHistorySession(session) },
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }
            }
        } else {
            AnalyticsView(sessions = state.sessions)
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

@Composable
fun AnalyticsView(sessions: List<WorkoutSession>) {
    val completedSessions = sessions.filter { it.isCompleted }
    val totalCompleted = completedSessions.size
    val totalLogged = sessions.size

    val pushCount = sessions.count { it.dayNumber == 1 || it.dayNumber == 4 }
    val pullCount = sessions.count { it.dayNumber == 2 || it.dayNumber == 5 }
    val legsCount = sessions.count { it.dayNumber == 3 || it.dayNumber == 6 }

    val sessionsByWeek = sessions.groupBy { GymProgram.absoluteWeekOf(it) }
        .toSortedMap()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnalyticsStatCard(
                label = "Completed",
                value = "$totalCompleted",
                subtitle = "of $totalLogged logged",
                color = GreenSuccess,
                modifier = Modifier.weight(1f)
            )
            AnalyticsStatCard(
                label = "Active Weeks",
                value = "${sessionsByWeek.keys.size}",
                subtitle = "weeks trained",
                color = Orange40,
                modifier = Modifier.weight(1f)
            )
            AnalyticsStatCard(
                label = "PPL Balance",
                value = "$pushCount / $pullCount / $legsCount",
                subtitle = "Push · Pull · Legs",
                color = BluePrevious,
                modifier = Modifier.weight(1f)
            )
        }

        // Weekly Activity Chart Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Workout Frequency",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Target: 6 days per week (PPL × 2)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (sessionsByWeek.isEmpty()) {
                    Text(
                        text = "Complete workouts to see weekly trends here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        sessionsByWeek.forEach { (week, weekSessions) ->
                            val doneCount = weekSessions.count { it.isCompleted }
                            val totalWeek = weekSessions.size
                            val fraction = (totalWeek / 6f).coerceIn(0.1f, 1f)
                            val isDeload = GymProgram.cycleWeekOf(week) == GymProgram.WEEKS_PER_BLOCK

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$totalWeek",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (doneCount == 6) GreenSuccess else Orange40,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(22.dp)
                                        .height((100 * fraction).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            if (isDeload) Amber
                                            else if (doneCount == 6) GreenSuccess
                                            else Orange40
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "W$week",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDeload) Amber else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Muscle Group Split Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Muscle Split Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                val totalSessions = (pushCount + pullCount + legsCount).coerceAtLeast(1)
                DistributionRow(
                    label = "Push (Chest · Shoulders · Triceps)",
                    count = pushCount,
                    total = totalSessions,
                    color = Orange40
                )
                Spacer(modifier = Modifier.height(8.dp))
                DistributionRow(
                    label = "Pull (Lats · Upper Back · Biceps)",
                    count = pullCount,
                    total = totalSessions,
                    color = BluePrevious
                )
                Spacer(modifier = Modifier.height(8.dp))
                DistributionRow(
                    label = "Legs (Quads · Hamstrings · Calves)",
                    count = legsCount,
                    total = totalSessions,
                    color = GreenSuccess
                )
            }
        }

        // Periodization & Progressive Overload Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Double Progression Guide",
                    style = MaterialTheme.typography.titleSmall,
                    color = Orange40,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "1. Keep the weight constant until you can hit the top rep target on all sets.\n" +
                            "2. When the badge \"Level Up\" appears, add weight next session (+2.5kg barbell, +2kg dumbbell, +5kg machine).\n" +
                            "3. Week 6 of each block is a Deload (50% sets) to dissipate accumulated fatigue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun AnalyticsStatCard(
    label: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DistributionRow(label: String, count: Int, total: Int, color: Color) {
    val pct = (count.toFloat() / total * 100).toInt()
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "$count ($pct%)", style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(count.toFloat() / total)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
