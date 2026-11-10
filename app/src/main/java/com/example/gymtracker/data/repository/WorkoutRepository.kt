package com.example.gymtracker.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.gymtracker.data.db.ExerciseSet
import com.example.gymtracker.data.db.GymDatabase
import com.example.gymtracker.data.db.WorkoutSession
import com.example.gymtracker.data.program.GymProgram
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class DayStatus { EMPTY, IN_PROGRESS, COMPLETED }

data class PreviousPerformance(val absoluteWeek: Int, val sets: List<ExerciseSet>)

fun ExerciseSet.hasData(): Boolean = weight != null || reps != null

class WorkoutRepository(private val context: Context) {

    private val db = GymDatabase.getDatabase(context)
    private val dao = db.workoutDao()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gym_tracker_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun getSelectedWeek(): Int = prefs.getInt("selected_week", 1)

    fun saveSelectedWeek(week: Int) {
        prefs.edit().putInt("selected_week", week).apply()
    }

    fun getSelectedDay(): Int = prefs.getInt("selected_day", 1)

    fun saveSelectedDay(day: Int) {
        prefs.edit().putInt("selected_day", day).apply()
    }

    fun getTodayDate(): String = dateFormat.format(Calendar.getInstance().time)

    /** The day after the most recently submitted workout, as (absoluteWeek, dayNumber). */
    suspend fun suggestNextWorkout(): Pair<Int, Int> {
        val last = dao.getAllSessionsList()
            .filter { it.isCompleted }
            .maxWithOrNull(compareBy({ GymProgram.absoluteWeekOf(it) }, { it.dayNumber }))
            ?: return Pair(1, 1)
        val week = GymProgram.absoluteWeekOf(last)
        return if (last.dayNumber >= 6) Pair(week + 1, 1) else Pair(week, last.dayNumber + 1)
    }

    suspend fun getOrCreateSession(absoluteWeek: Int, dayNumber: Int): WorkoutSession? {
        val programDay = GymProgram.getDayByNumber(dayNumber) ?: return null
        val week = GymProgram.cycleWeekOf(absoluteWeek)
        val block = GymProgram.blockOf(absoluteWeek)

        val session = dao.getSession(dayNumber, week, block)
            ?: dao.getSessionById(
                dao.insertSession(
                    WorkoutSession(
                        dayNumber = dayNumber,
                        weekNumber = week,
                        blockNumber = block,
                        date = getTodayDate()
                    )
                )
            )
            ?: return null

        // Top up any sets the program calls for this week that don't exist yet.
        // Submitted sessions are left exactly as they were logged.
        if (!session.isCompleted) {
            val existing = dao.getExerciseSetsForSession(session.id)
            val missing = mutableListOf<ExerciseSet>()
            for (exercise in programDay.exercises) {
                val have = existing.count { it.exerciseId == exercise.id }
                val target = GymProgram.getSetsForWeek(exercise, week)
                for (s in have + 1..target) {
                    missing.add(
                        ExerciseSet(
                            sessionId = session.id,
                            exerciseId = exercise.id,
                            exerciseName = exercise.name,
                            setNumber = s
                        )
                    )
                }
            }
            if (missing.isNotEmpty()) dao.insertExerciseSets(missing)
        }
        return session
    }

    suspend fun getSessionById(sessionId: Long): WorkoutSession? = dao.getSessionById(sessionId)

    suspend fun getExerciseSetsForSession(sessionId: Long): List<ExerciseSet> =
        dao.getExerciseSetsForSession(sessionId)

    fun getExerciseSetsForSessionFlow(sessionId: Long): Flow<List<ExerciseSet>> =
        dao.getExerciseSetsForSessionFlow(sessionId)

    /**
     * For each exercise of [dayNumber], the most recent week before [absoluteWeek]
     * in which it was actually logged — so skipped weeks fall back to the last real effort.
     */
    suspend fun getPreviousPerformance(absoluteWeek: Int, dayNumber: Int): Map<String, PreviousPerformance> {
        val programDay = GymProgram.getDayByNumber(dayNumber) ?: return emptyMap()
        val earlier = dao.getAllSessionsList()
            .filter { it.dayNumber == dayNumber && GymProgram.absoluteWeekOf(it) < absoluteWeek }
            .sortedWith(compareByDescending<WorkoutSession> { GymProgram.absoluteWeekOf(it) }
                .thenByDescending { it.isCompleted })
        val setsBySession = dao.getAllExerciseSets().groupBy { it.sessionId }

        val result = mutableMapOf<String, PreviousPerformance>()
        for (exercise in programDay.exercises) {
            for (session in earlier) {
                val logged = setsBySession[session.id].orEmpty()
                    .filter { it.exerciseId == exercise.id && it.reps != null }
                if (logged.isNotEmpty()) {
                    result[exercise.id] = PreviousPerformance(GymProgram.absoluteWeekOf(session), logged)
                    break
                }
            }
        }
        return result
    }

    suspend fun getDayStatuses(absoluteWeek: Int): Map<Int, DayStatus> {
        val week = GymProgram.cycleWeekOf(absoluteWeek)
        val block = GymProgram.blockOf(absoluteWeek)
        return (1..6).associateWith { day ->
            val session = dao.getSession(day, week, block)
            when {
                session == null -> DayStatus.EMPTY
                session.isCompleted -> DayStatus.COMPLETED
                dao.getExerciseSetsForSession(session.id).any { it.hasData() } -> DayStatus.IN_PROGRESS
                else -> DayStatus.EMPTY
            }
        }
    }

    /** Removes a session that was only opened, never filled in. */
    suspend fun deleteSessionIfEmpty(sessionId: Long) {
        val session = dao.getSessionById(sessionId) ?: return
        if (session.isCompleted || session.notes.isNotBlank()) return
        val sets = dao.getExerciseSetsForSession(session.id)
        if (sets.any { it.hasData() || it.isCompleted }) return
        deleteSession(session.id)
    }

    suspend fun cleanupEmptySessions() {
        for (session in dao.getAllSessionsList()) deleteSessionIfEmpty(session.id)
    }

    suspend fun updateExerciseSet(set: ExerciseSet) = dao.updateExerciseSet(set)

    suspend fun updateSession(session: WorkoutSession) = dao.updateSession(session)

    suspend fun deleteSession(sessionId: Long) {
        dao.deleteExerciseSetsForSession(sessionId)
        dao.deleteSession(sessionId)
    }

    fun getAllSessions(): Flow<List<WorkoutSession>> = dao.getAllSessions()

    suspend fun exportData(): String {
        val sessions = dao.getAllSessionsList()
        val allSets = dao.getAllExerciseSets()
        val exportData = mapOf(
            "sessions" to sessions,
            "exerciseSets" to allSets
        )
        return Gson().toJson(exportData)
    }

    suspend fun saveBackupToFile(): File {
        val json = exportData()
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        backupDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Calendar.getInstance().time)
        val file = File(backupDir, "gym_tracker_backup_$timestamp.json")
        file.writeText(json)
        return file
    }

    suspend fun importData(json: String) {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = Gson().fromJson(json, type)

        dao.deleteAllExerciseSets()
        dao.deleteAllSessions()

        val sessionsJson = Gson().toJson(data["sessions"])
        val sessionsType = object : TypeToken<List<WorkoutSession>>() {}.type
        val sessions: List<WorkoutSession> = Gson().fromJson(sessionsJson, sessionsType)
        for (session in sessions) {
            dao.insertSession(session)
        }

        val setsJson = Gson().toJson(data["exerciseSets"])
        val setsType = object : TypeToken<List<ExerciseSet>>() {}.type
        val sets: List<ExerciseSet> = Gson().fromJson(setsJson, setsType)
        dao.insertExerciseSets(sets)
    }

    suspend fun clearAllData() {
        dao.deleteAllExerciseSets()
        dao.deleteAllSessions()
        prefs.edit().remove("selected_week").remove("selected_day").apply()
    }
}
