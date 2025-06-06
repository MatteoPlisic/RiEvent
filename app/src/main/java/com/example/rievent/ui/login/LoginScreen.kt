package com.example.rievent.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.rievent.R

// The preview might need an update to pass the new onBack lambda
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    // A simplified preview that doesn't rely on a real ViewModel or NavController
    // Note: To properly preview, you'd mock the state and callbacks.
    val previewState = LoginUiState(email = "test@example.com", password = "password123")
    // LoginScreen(
    //     state = previewState,
    //     onEmailChange = {},
    //     onPasswordChange = {},
    //     onLoginClick = {},
    //     onForgotPasswordClick = {},
    //     onBack = {}, // Add the new onBack lambda
    //     viewModel = null, // Or a mock ViewModel
    //     navController = NavController(LocalContext.current)
    // )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onBack: () -> Unit, // Add the new parameter
    viewModel: LoginViewModel,
    navController: NavController,
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            navController.navigate("home") {
                popUpTo("welcome") { inclusive = true }
            }
            viewModel.clearNavigationFlag()
        }
    }

    // [THE FIX] - Wrap the content in a Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Title can be empty for a clean look */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from the Scaffold
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ri_event_logo),
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier.size(200.dp)
            )

            Text(
                text = stringResource(R.string.login_title),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.login_description),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(20.dp)) // Added more space for better layout

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text(text = stringResource(R.string.email)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text(text = stringResource(R.string.password)) },
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier = Modifier.height(20.dp)) // Added more space

            Button(onClick = onLoginClick) {
                Text(text = stringResource(R.string.login_button))
            }

            TextButton(onClick = onForgotPasswordClick) {
                Text(text = stringResource(R.string.forgot_password))
            }
        }
    }
}