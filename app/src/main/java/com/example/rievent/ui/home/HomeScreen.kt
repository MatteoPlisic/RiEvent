package com.example.rievent.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.rievent.R
import com.example.rievent.ui.utils.Drawer

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    navController: NavHostController,
) {
    Drawer(
        title = stringResource(id = R.string.welcome_home_button),
        navController = navController,
        gesturesEnabled = true,
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
                Text(
                    text = stringResource(id = R.string.home_screen_welcome_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(id = R.string.home_screen_logged_in_message),
                    fontSize = 18.sp
                )
                Button(onClick = onLogout) {
                    Text(stringResource(id = R.string.home_screen_logout_button))
                }
            }
        }
    }
}