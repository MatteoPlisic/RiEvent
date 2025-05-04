package com.example.rievent.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rievent.ui.utils.Drawer


@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToMyEvents: () -> Unit
) {
    Drawer(
        title = "Home",
        onLogout = onLogout,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToMyEvents = onNavigateToMyEvents
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Welcome Home!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(text = "You're now logged in.", fontSize = 18.sp)
                Button(onClick = onLogout) {
                    Text("Log out")
                }
            }
        }
    }
}
