package com.example.rievent.ui.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Drawer(
    title: String,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToCreateEvent: () -> Unit = {},
    onNavigateToMyEvents: () -> Unit = {},
    onLogout: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menu", modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(label = { Text("Home") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                })
                NavigationDrawerItem(label = { Text("Events") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToEvents()
                })
                NavigationDrawerItem(label = { Text("Profile") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToProfile()
                })
                NavigationDrawerItem(label = { Text("Create event") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToCreateEvent()
                })
                NavigationDrawerItem(label = { Text("My events") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    onNavigateToMyEvents()
                })
                NavigationDrawerItem(label = { Text("Log out") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    onLogout()
                    FirebaseAuth.getInstance().signOut()

                })
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            content = content
        )
    }
}
