package com.example.gymtracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gymtracker.ui.screens.BackupScreen
import com.example.gymtracker.ui.screens.HistoryScreen
import com.example.gymtracker.ui.screens.SplashScreen
import com.example.gymtracker.ui.screens.WorkoutScreen
import com.example.gymtracker.ui.theme.CardDark
import com.example.gymtracker.ui.theme.DarkBackground
import com.example.gymtracker.ui.theme.Orange40
import com.example.gymtracker.ui.theme.TextMuted
import com.example.gymtracker.ui.viewmodel.WorkoutViewModel

enum class BottomNavItem(val label: String, val icon: ImageVector) {
    WORKOUT("Workout", Icons.Default.FitnessCenter),
    HISTORY("History", Icons.Default.History),
    BACKUP("Backup", Icons.Default.Backup)
}

@Composable
fun GymTrackerNavHost(viewModel: WorkoutViewModel) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onSplashComplete = { showSplash = false })
        return
    }

    var selectedTab by rememberSaveable { mutableStateOf(BottomNavItem.WORKOUT) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = CardDark,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                BottomNavItem.entries.forEach { item ->
                    NavigationBarItem(
                        selected = selectedTab == item,
                        onClick = {
                            selectedTab = item
                            if (item == BottomNavItem.WORKOUT) {
                                viewModel.refreshWorkout()
                            }
                        },
                        icon = {
                            Icon(item.icon, contentDescription = item.label)
                        },
                        label = {
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Orange40,
                            selectedTextColor = Orange40,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = Orange40.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(DarkBackground)
        ) {
            when (selectedTab) {
                BottomNavItem.WORKOUT -> WorkoutScreen(viewModel)
                BottomNavItem.HISTORY -> HistoryScreen(viewModel)
                BottomNavItem.BACKUP -> BackupScreen(viewModel)
            }
        }
    }
}
