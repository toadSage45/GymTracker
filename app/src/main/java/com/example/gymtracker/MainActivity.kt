package com.example.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gymtracker.ui.navigation.GymTrackerNavHost
import com.example.gymtracker.ui.theme.DarkBackground
import com.example.gymtracker.ui.theme.GymTrackerTheme
import com.example.gymtracker.ui.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val viewModel: WorkoutViewModel = viewModel()
                    GymTrackerNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
