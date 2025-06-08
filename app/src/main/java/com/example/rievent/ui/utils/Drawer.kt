package com.example.rievent.ui.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Drawer(
    title: String,
    navController: NavController,
    gesturesEnabled: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = gesturesEnabled,
        drawerContent = {
            ModalDrawerSheet {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Menu", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)

                    IconButton(onClick = {
                        scope.launch { drawerState.close() }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close Drawer"
                        )
                    }
                }


                NavigationDrawerItem(label = { Text("Events") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("events")
                })
                NavigationDrawerItem(label = { Text("Profile") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("profile/${FirebaseAuth.getInstance().currentUser?.uid}")
                })
                NavigationDrawerItem(label = { Text("Create event") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("createEvent")
                })
                NavigationDrawerItem(label = { Text("My events") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("myEvents")
                })
                NavigationDrawerItem(label = { Text("Map") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("eventMap")
                })
                NavigationDrawerItem(label = { Text("Messages") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("messages")
                })
                NavigationDrawerItem(label = { Text("Log out") }, selected = false, onClick = {
                    scope.launch { drawerState.close() }
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("welcome") {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
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