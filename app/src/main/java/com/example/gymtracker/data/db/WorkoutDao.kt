package com.example.gymtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseSets(sets: List<ExerciseSet>)

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Update
    suspend fun updateExerciseSet(set: ExerciseSet)

    @Query("SELECT * FROM workout_sessions WHERE dayNumber = :dayNumber AND weekNumber = :weekNumber AND blockNumber = :blockNumber ORDER BY isCompleted DESC, id DESC LIMIT 1")
    suspend fun getSession(dayNumber: Int, weekNumber: Int, blockNumber: Int): WorkoutSession?

    @Query("SELECT * FROM workout_sessions ORDER BY blockNumber DESC, weekNumber DESC, dayNumber DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber")
    suspend fun getExerciseSetsForSession(sessionId: Long): List<ExerciseSet>

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY exerciseId, setNumber")
    fun getExerciseSetsForSessionFlow(sessionId: Long): Flow<List<ExerciseSet>>

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId AND exerciseId = :exerciseId ORDER BY setNumber")
    suspend fun getExerciseSetsForExercise(sessionId: Long, exerciseId: String): List<ExerciseSet>

    @Query("SELECT * FROM workout_sessions ORDER BY blockNumber DESC, weekNumber DESC, dayNumber DESC")
    suspend fun getAllSessionsList(): List<WorkoutSession>

    @Query("SELECT * FROM exercise_sets ORDER BY sessionId, exerciseId, setNumber")
    suspend fun getAllExerciseSets(): List<ExerciseSet>

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM exercise_sets")
    suspend fun deleteAllExerciseSets()

    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    @Query("DELETE FROM exercise_sets WHERE sessionId = :sessionId")
    suspend fun deleteExerciseSetsForSession(sessionId: Long)

    @Query("""
        SELECT * FROM workout_sessions 
        WHERE dayNumber = :dayNumber 
          AND ((blockNumber - 1) * 6 + weekNumber) < :absoluteWeek
          AND id IN (
              SELECT DISTINCT sessionId FROM exercise_sets WHERE reps IS NOT NULL
          )
        ORDER BY ((blockNumber - 1) * 6 + weekNumber) DESC, isCompleted DESC, id DESC 
        LIMIT 6
    """)
    suspend fun getPreviousSessionsForDay(dayNumber: Int, absoluteWeek: Int): List<WorkoutSession>

    @Query("SELECT * FROM exercise_sets WHERE sessionId IN (:sessionIds) ORDER BY sessionId, exerciseId, setNumber")
    suspend fun getExerciseSetsForSessions(sessionIds: List<Long>): List<ExerciseSet>

    @Query("""
        DELETE FROM workout_sessions 
        WHERE isCompleted = 0 
          AND (notes IS NULL OR TRIM(notes) = '') 
          AND id NOT IN (
              SELECT DISTINCT sessionId FROM exercise_sets 
              WHERE weight IS NOT NULL OR reps IS NOT NULL OR isCompleted = 1
          )
    """)
    suspend fun cleanupEmptySessionsFast()

    @Query("DELETE FROM exercise_sets WHERE sessionId NOT IN (SELECT id FROM workout_sessions)")
    suspend fun cleanupOrphanExerciseSets()
}
