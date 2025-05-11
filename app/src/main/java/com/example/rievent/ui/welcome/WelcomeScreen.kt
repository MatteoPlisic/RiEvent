package com.example.rievent.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rievent.R
import androidx.compose.ui.text.font.FontStyle
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    viewModel: WelcomeViewModel,
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()


    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            navController.navigate("home") {
                popUpTo("welcome") { inclusive = true }
            }
            viewModel.clearNavigationFlag()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Menu", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold)
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = false,
                    onClick = {
                        navController.navigate("home")
                    }
                )
                NavigationDrawerItem(
                    label = { Text("Login") },
                    selected = false,
                    onClick = onLoginClick
                )
                NavigationDrawerItem(
                    label = { Text("Register") },
                    selected = false,
                    onClick = onRegisterClick
                )
            }
        }
    ) {
        // Your original WelcomeScreen UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ri_event_logo),
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.home_text),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Login")
                }
                Button(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Register")
                }
                Button(
                    onClick = { viewModel.requestGoogleIdToken() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Login with Google")
                }
            }

            Text(
                text = "RiEvent is an app for public events in greater Rijeka area. Join us to find events near you",
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 250.dp)
            )
        }
    }
}
