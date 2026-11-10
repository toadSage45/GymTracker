package com.example.gymtracker.ui.viewmodel

import android.app.Application
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseUiState(
    val programExercise: ProgramExercise,
    val currentSets: List<ExerciseSet>,
    val previousSets: List<ExerciseSet>,
    val isCompleted: Boolean,
    /** Absolute week the previous sets come from, or null if never logged before. */
    val previousWeek: Int? = null
)

data class WorkoutUiState(
    val isLoading: Boolean = true,
    /** Continuous week number: 1, 2, ... 7 = block 2 week 1, etc. */
    val selectedWeek: Int = 1,
    val selectedDay: Int = 1,
    val dayStatuses: Map<Int, DayStatus> = emptyMap(),
    val session: WorkoutSession? = null,
    val programDay: ProgramDay? = null,
    val exercises: List<ExerciseUiState> = emptyList(),
    val allCompleted: Boolean = false,
    val showExerciseDetail: ProgramExercise? = null,
    val showNotes: Boolean = false
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

    init {
        viewModelScope.launch {
            repository.cleanupEmptySessions()
            val week = repository.getSelectedWeek()
            val day = repository.getSelectedDay()
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
            if (programDay == null || session == null) {
                _workoutState.value = _workoutState.value.copy(
                    isLoading = false, session = null, programDay = null, exercises = emptyList()
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
                dayStatuses = repository.getDayStatuses(week),
                session = session,
                programDay = programDay,
                exercises = exercises,
                allCompleted = allDone(exercises)
            )
        }
    }

    private fun buildExercises(
        programDay: ProgramDay,
        currentSets: List<ExerciseSet>,
        previous: Map<String, PreviousPerformance>
    ): List<ExerciseUiState> = programDay.exercises.map { exercise ->
        val current = currentSets.filter { it.exerciseId == exercise.id }
        val prev = previous[exercise.id]
        ExerciseUiState(
            programExercise = exercise,
            currentSets = current,
            previousSets = prev?.sets.orEmpty(),
            isCompleted = current.isNotEmpty() && current.all { it.isCompleted },
            previousWeek = prev?.absoluteWeek
        )
    }

    private fun allDone(exercises: List<ExerciseUiState>) =
        exercises.all { it.isCompleted || it.programExercise.isTimedHold }

    fun updateSet(set: ExerciseSet, weight: Float?, reps: Int?) {
        viewModelScope.launch {
            val updated = set.copy(weight = weight, reps = reps)
            repository.updateExerciseSet(updated)
            refreshCurrentSets()
        }
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
            // Stamp the date the workout was actually done when submitting.
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
