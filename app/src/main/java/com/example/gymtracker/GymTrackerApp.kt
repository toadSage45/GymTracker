package com.example.gymtracker

import android.app.Application
import com.example.gymtracker.data.repository.WorkoutRepository

class GymTrackerApp : Application() {

    lateinit var repository: WorkoutRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = WorkoutRepository(this)
    }
}
