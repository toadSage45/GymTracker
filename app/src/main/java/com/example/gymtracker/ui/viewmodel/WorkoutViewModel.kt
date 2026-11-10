package com.example.gymtracker.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.GymTrackerApp
import com.example.gymtracker.data.db.ExerciseSet
import com.example.gymtracker.data.db.WorkoutSession
import com.example.gymtracker.data.program.GymProgram
import com.example.gymtracker.data.program.ProgramDay
import com.example.gymtracker.data.program.ProgramExercise
import com.example.gymtracker.data.repository.DayStatus
import com.example.gymtracker.data.repository.PreviousPerformance
import com.example.gymtracker.data.repository.hasData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

data class ExerciseUiState(
    val programExercise: ProgramExercise,
    val currentSets: List<ExerciseSet>,
    val previousSets: List<ExerciseSet>,
    val isCompleted: Boolean,
    /** Absolute week the previous sets come from, or null if never logged before. */
    val previousWeek: Int? = null,
    val doubleProgressionEligible: Boolean = false,
    val doubleProgressionJump: String? = null
)

data class TimerUiState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val label: String = ""
)

data class WorkoutUiState(
    val isLoading: Boolean = true,
    val selectedWeek: Int = 1,
    val selectedDay: Int = 1,
    val suggestedNext: Pair<Int, Int>? = null,
    val dayStatuses: Map<Int, DayStatus> = emptyMap(),
    val session: WorkoutSession? = null,
    val programDay: ProgramDay? = null,
    val exercises: List<ExerciseUiState> = emptyList(),
    val allCompleted: Boolean = false,
    val showExerciseDetail: ProgramExercise? = null,
    val showNotes: Boolean = false,
    val timerState: TimerUiState = TimerUiState()
)

data class HistoryUiState(
    val sessions: List<WorkoutSession> = emptyList(),
    val selectedSession: WorkoutSession? = null,
    val selectedExercises: List<ExerciseUiState> = emptyList(),
    val selectedProgramDay: ProgramDay? = null,
    val isEditing: Boolean = false
)

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastBackupPath: String? = null,
    val message: String? = null
)

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GymTrackerApp
    private val repository = app.repository

    private val _workoutState = MutableStateFlow(WorkoutUiState())
    val workoutState: StateFlow<WorkoutUiState> = _workoutState.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryUiState())
    val historyState: StateFlow<HistoryUiState> = _historyState.asStateFlow()

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    private var loadJob: Job? = null
    private var timerJob: Job? = null
    private val pendingDebounceJobs = ConcurrentHashMap<Long, Job>()

    init {
        viewModelScope.launch {
            repository.cleanupEmptySessions()
            val (week, day) = repository.getInitialWorkout()
            val suggested = repository.suggestNextWorkout()
            _workoutState.value = _workoutState.value.copy(suggestedNext = suggested)
            loadWorkout(week, day)
        }
        loadHistory()
    }

    fun selectWeek(week: Int) {
        if (week < 1) return
        repository.saveSelectedWeek(week)
        switchTo(week, _workoutState.value.selectedDay)
    }

    fun selectDay(day: Int) {
        if (day !in 1..6) return
        repository.saveSelectedDay(day)
        switchTo(_workoutState.value.selectedWeek, day)
    }

    fun jumpToNextWorkout() {
        viewModelScope.launch {
            val (nextWeek, nextDay) = repository.suggestNextWorkout()
            selectWeek(nextWeek)
            selectDay(nextDay)
        }
    }

    private fun switchTo(week: Int, day: Int) {
        val state = _workoutState.value
        if (week == state.selectedWeek && day == state.selectedDay && state.session != null) return
        _workoutState.value = state.copy(selectedWeek = week, selectedDay = day)
        loadWorkout(week, day, leavingSessionId = state.session?.id)
    }

    /** Reloads whatever week/day is currently selected. */
    fun refreshWorkout() {
        val state = _workoutState.value
        if (state.isLoading) return
        loadWorkout(state.selectedWeek, state.selectedDay)
    }

    private fun loadWorkout(week: Int, day: Int, leavingSessionId: Long? = null) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Don't leave behind empty sessions for days that were only browsed.
            if (leavingSessionId != null) repository.deleteSessionIfEmpty(leavingSessionId)

            val programDay = GymProgram.getDayByNumber(day)
            val session = repository.getOrCreateSession(week, day)
            val suggested = repository.suggestNextWorkout()
            if (programDay == null || session == null) {
                _workoutState.value = _workoutState.value.copy(
                    isLoading = false, session = null, programDay = null, exercises = emptyList(),
                    suggestedNext = suggested
                )
                return@launch
            }

            val exercises = buildExercises(
                programDay,
                repository.getExerciseSetsForSession(session.id),
                repository.getPreviousPerformance(week, day)
            )

            _workoutState.value = _workoutState.value.copy(
                isLoading = false,
                selectedWeek = week,
                selectedDay = day,
                suggestedNext = suggested,
                dayStatuses = repository.getDayStatuses(week),
                session = session,
                programDay = programDay,
                exercises = exercises,
                allCompleted = allDone(exercises)
            )
        }
    }

    private fun computeDoubleProgression(
        exercise: ProgramExercise,
        previousSets: List<ExerciseSet>
    ): Pair<Boolean, String?> {
        if (exercise.isTimedHold || previousSets.isEmpty()) return Pair(false, null)
        val repNumbers = Regex("\\d+").findAll(exercise.repRange).map { it.value.toInt() }.toList()
        val topRep = repNumbers.lastOrNull() ?: return Pair(false, null)

        val allHitTop = previousSets.all { (it.reps ?: 0) >= topRep && (it.weight ?: 0f) > 0f }
        if (!allHitTop) return Pair(false, null)

        val nameLower = exercise.name.lowercase()
        val jump = when {
            nameLower.contains("barbell") || nameLower.contains("bench press") || nameLower.contains("deadlift") || nameLower.contains("squat") -> "+2.5 kg"
            nameLower.contains("dumbbell") || nameLower.contains("db") -> "+2 kg"
            nameLower.contains("cable") || nameLower.contains("machine") || nameLower.contains("pulldown") || nameLower.contains("leg press") -> "+5 kg / 1 pin"
            else -> "+2.5 kg"
        }
        return Pair(true, jump)
    }

    private fun buildExercises(
        programDay: ProgramDay,
        currentSets: List<ExerciseSet>,
        previous: Map<String, PreviousPerformance>
    ): List<ExerciseUiState> = programDay.exercises.map { exercise ->
        val current = currentSets.filter { it.exerciseId == exercise.id }
        val prev = previous[exercise.id]
        val (eligible, jump) = computeDoubleProgression(exercise, prev?.sets.orEmpty())
        ExerciseUiState(
            programExercise = exercise,
            currentSets = current,
            previousSets = prev?.sets.orEmpty(),
            isCompleted = current.isNotEmpty() && current.all { it.isCompleted },
            previousWeek = prev?.absoluteWeek,
            doubleProgressionEligible = eligible,
            doubleProgressionJump = jump
        )
    }

    private fun allDone(exercises: List<ExerciseUiState>) =
        exercises.all { it.isCompleted || it.programExercise.isTimedHold }

    fun updateSet(set: ExerciseSet, weight: Float?, reps: Int?) {
        val updated = set.copy(weight = weight, reps = reps)
        updateSetInMemory(updated)

        pendingDebounceJobs[set.id]?.cancel()
        pendingDebounceJobs[set.id] = viewModelScope.launch {
            delay(300)
            repository.updateExerciseSet(updated)
            updateDayStatusInMemory()
        }
    }

    fun updateAllSetsWeight(exerciseId: String, weight: Float?) {
        val state = _workoutState.value
        val ex = state.exercises.find { it.programExercise.id == exerciseId } ?: return
        val updatedSets = ex.currentSets.map { it.copy(weight = weight) }

        val updatedExercises = state.exercises.map {
            if (it.programExercise.id == exerciseId) it.copy(currentSets = updatedSets) else it
        }
        _workoutState.value = state.copy(exercises = updatedExercises)

        viewModelScope.launch {
            for (s in updatedSets) {
                repository.updateExerciseSet(s)
            }
            updateDayStatusInMemory()
        }
    }

    fun toggleSetComplete(setId: Long) {
        val state = _workoutState.value
        var toggledSet: ExerciseSet? = null
        val updatedExercises = state.exercises.map { ex ->
            val setExists = ex.currentSets.any { it.id == setId }
            if (setExists) {
                val updatedSets = ex.currentSets.map { s ->
                    if (s.id == setId) {
                        val newComplete = !s.isCompleted
                        toggledSet = s.copy(isCompleted = newComplete)
                        toggledSet!!
                    } else s
                }
                ex.copy(
                    currentSets = updatedSets,
                    isCompleted = updatedSets.isNotEmpty() && updatedSets.all { it.isCompleted }
                )
            } else ex
        }

        _workoutState.value = state.copy(
            exercises = updatedExercises,
            allCompleted = allDone(updatedExercises)
        )

        toggledSet?.let { s ->
            viewModelScope.launch {
                repository.updateExerciseSet(s)
                updateDayStatusInMemory()
            }
            if (s.isCompleted) {
                val ex = state.exercises.find { it.currentSets.any { set -> set.id == setId } }
                if (ex != null && !state.timerState.isRunning) {
                    val secs = parseRestTimeSeconds(ex.programExercise.restTime)
                    startTimer(secs, "${ex.programExercise.name} Rest")
                }
            }
        }
    }

    private fun updateSetInMemory(updated: ExerciseSet) {
        val state = _workoutState.value
        val updatedExercises = state.exercises.map { ex ->
            val setExists = ex.currentSets.any { it.id == updated.id }
            if (setExists) {
                val updatedSets = ex.currentSets.map { if (it.id == updated.id) updated else it }
                ex.copy(
                    currentSets = updatedSets,
                    isCompleted = updatedSets.isNotEmpty() && updatedSets.all { it.isCompleted }
                )
            } else ex
        }
        _workoutState.value = state.copy(
            exercises = updatedExercises,
            allCompleted = allDone(updatedExercises)
        )
    }

    private suspend fun updateDayStatusInMemory() {
        val state = _workoutState.value
        val session = state.session ?: return
        val currentSets = repository.getExerciseSetsForSession(session.id)
        val status = when {
            session.isCompleted -> DayStatus.COMPLETED
            currentSets.any { it.hasData() } -> DayStatus.IN_PROGRESS
            else -> DayStatus.EMPTY
        }
        _workoutState.value = state.copy(
            dayStatuses = state.dayStatuses + (state.selectedDay to status)
        )
    }

    fun toggleExerciseComplete(exerciseId: String) {
        viewModelScope.launch {
            val session = _workoutState.value.session ?: return@launch
            val sets = repository.getExerciseSetsForSession(session.id)
                .filter { it.exerciseId == exerciseId }
            val allCompleted = sets.isNotEmpty() && sets.all { it.isCompleted }
            val newState = !allCompleted
            for (set in sets) {
                repository.updateExerciseSet(set.copy(isCompleted = newState))
            }
            refreshCurrentSets()
        }
    }

    fun toggleSubmitWorkout() {
        viewModelScope.launch {
            val session = _workoutState.value.session ?: return@launch
            val submitting = !session.isCompleted
            repository.updateSession(
                if (submitting) session.copy(isCompleted = true, date = repository.getTodayDate())
                else session.copy(isCompleted = false)
            )
            refreshWorkout()
        }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch {
            val session = _workoutState.value.session ?: return@launch
            repository.updateSession(session.copy(notes = notes))
            _workoutState.value = _workoutState.value.copy(
                session = session.copy(notes = notes)
            )
        }
    }

    fun showExerciseDetail(exercise: ProgramExercise?) {
        _workoutState.value = _workoutState.value.copy(showExerciseDetail = exercise)
    }

    fun toggleNotes() {
        _workoutState.value = _workoutState.value.copy(showNotes = !_workoutState.value.showNotes)
    }

    private suspend fun refreshCurrentSets() {
        val state = _workoutState.value
        val session = state.session ?: return
        val currentSets = repository.getExerciseSetsForSession(session.id)

        val exercises = state.exercises.map { ex ->
            val current = currentSets.filter { it.exerciseId == ex.programExercise.id }
            ex.copy(
                currentSets = current,
                isCompleted = current.isNotEmpty() && current.all { it.isCompleted }
            )
        }
        val status = when {
            session.isCompleted -> DayStatus.COMPLETED
            currentSets.any { it.hasData() } -> DayStatus.IN_PROGRESS
            else -> DayStatus.EMPTY
        }

        _workoutState.value = _workoutState.value.copy(
            exercises = exercises,
            allCompleted = allDone(exercises),
            dayStatuses = state.dayStatuses + (state.selectedDay to status)
        )
    }

    fun startTimer(seconds: Int, label: String = "Rest Timer") {
        timerJob?.cancel()
        _workoutState.value = _workoutState.value.copy(
            timerState = TimerUiState(
                isRunning = true,
                isPaused = false,
                totalSeconds = seconds,
                remainingSeconds = seconds,
                label = label
            )
        )
        timerJob = viewModelScope.launch {
            while (_workoutState.value.timerState.remainingSeconds > 0) {
                delay(1000)
                if (!_workoutState.value.timerState.isPaused) {
                    val currentRem = _workoutState.value.timerState.remainingSeconds - 1
                    _workoutState.value = _workoutState.value.copy(
                        timerState = _workoutState.value.timerState.copy(remainingSeconds = currentRem)
                    )
                }
            }
            triggerTimerVibration()
            _workoutState.value = _workoutState.value.copy(
                timerState = _workoutState.value.timerState.copy(isRunning = false, remainingSeconds = 0)
            )
        }
    }

    fun addTimerSeconds(seconds: Int = 15) {
        val current = _workoutState.value.timerState
        if (current.isRunning) {
            _workoutState.value = _workoutState.value.copy(
                timerState = current.copy(
                    remainingSeconds = current.remainingSeconds + seconds,
                    totalSeconds = maxOf(current.totalSeconds, current.remainingSeconds + seconds)
                )
            )
        }
    }

    fun toggleTimerPause() {
        val current = _workoutState.value.timerState
        if (current.isRunning) {
            _workoutState.value = _workoutState.value.copy(
                timerState = current.copy(isPaused = !current.isPaused)
            )
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _workoutState.value = _workoutState.value.copy(
            timerState = TimerUiState()
        )
    }

    private fun triggerTimerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 350, 150, 350), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(600)
            }
        } catch (_: Exception) {}
    }

    fun parseRestTimeSeconds(restTime: String): Int {
        val lower = restTime.lowercase()
        return when {
            lower.contains("3 min") -> 180
            lower.contains("2-3 min") -> 150
            lower.contains("2 min") -> 120
            lower.contains("90 sec") -> 90
            lower.contains("60 sec") -> 60
            lower.contains("30-45 sec") -> 45
            lower.contains("20-30s hold") -> 30
            lower.contains("hold") -> 30
            else -> 90
        }
    }

    fun loadHistory() {
        viewModelScope.launch {
            repository.getAllSessions().collect { sessions ->
                _historyState.value = _historyState.value.copy(sessions = sessions)
            }
        }
    }

    fun selectHistorySession(session: WorkoutSession) {
        viewModelScope.launch {
            val programDay = GymProgram.getDayByNumber(session.dayNumber)
            val sets = repository.getExerciseSetsForSession(session.id)
            val previous = repository.getPreviousPerformance(
                GymProgram.absoluteWeekOf(session), session.dayNumber
            )
            val exercises = programDay?.let { buildExercises(it, sets, previous) } ?: emptyList()

            _historyState.value = _historyState.value.copy(
                selectedSession = session,
                selectedExercises = exercises,
                selectedProgramDay = programDay,
                isEditing = true
            )
        }
    }

    fun updateHistorySet(set: ExerciseSet, weight: Float?, reps: Int?) {
        viewModelScope.launch {
            val updated = set.copy(weight = weight, reps = reps)
            repository.updateExerciseSet(updated)
            val session = _historyState.value.selectedSession
            if (session != null) selectHistorySession(session)
        }
    }

    fun updateHistoryNotes(notes: String) {
        viewModelScope.launch {
            val session = _historyState.value.selectedSession ?: return@launch
            repository.updateSession(session.copy(notes = notes))
            _historyState.value = _historyState.value.copy(
                selectedSession = session.copy(notes = notes)
            )
        }
    }

    fun deleteHistorySession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteSession(session.id)
            _historyState.value = _historyState.value.copy(
                selectedSession = null,
                selectedExercises = emptyList(),
                isEditing = false
            )
            refreshWorkout()
        }
    }

    fun closeHistoryEdit() {
        _historyState.value = _historyState.value.copy(
            selectedSession = null,
            selectedExercises = emptyList(),
            isEditing = false
        )
    }

    fun prepareExportData(onReady: (String) -> Unit) {
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(isExporting = true)
            try {
                val json = repository.exportData()
                onReady(json)
            } catch (e: Exception) {
                _backupState.value = BackupUiState(
                    isExporting = false,
                    message = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun importBackupData(json: String) {
        viewModelScope.launch {
            _backupState.value = _backupState.value.copy(isImporting = true)
            try {
                repository.importData(json)
                _backupState.value = BackupUiState(
                    isImporting = false,
                    message = "Backup restored successfully!"
                )
                refreshWorkout()
                loadHistory()
            } catch (e: Exception) {
                _backupState.value = BackupUiState(
                    isImporting = false,
                    message = "Restore failed: ${e.localizedMessage ?: "Invalid file"}"
                )
            }
        }
    }

    fun onExportComplete(success: Boolean, location: String?) {
        _backupState.value = BackupUiState(
            isExporting = false,
            lastBackupPath = location,
            message = if (success) "Backup saved successfully" else "Backup cancelled"
        )
    }

    fun clearBackupMessage() {
        _backupState.value = _backupState.value.copy(message = null)
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.clearAllData()
            loadWorkout(1, 1)
            loadHistory()
            onComplete()
        }
    }
}
