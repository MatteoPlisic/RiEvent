// ui/login/LoginScreen.kt
package com.example.rievent.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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



@Composable
fun LoginScreenPreview() {
    LoginScreen(
        state = LoginUiState(
            email = "test@example.com",
            password = "password123"
        ),
        onEmailChange = {},
        onPasswordChange = {},
        onLoginClick = {},
        onForgotPasswordClick = {},
        viewModel = LoginViewModel(),
        navController = TODO(),
    )
}


@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    viewModel: LoginViewModel,
    navController: NavController,
) {
    var scrollState = rememberScrollState()

    val uiState by viewModel.uiState.collectAsState()

    // ✅ Navigation side-effect
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            navController.navigate("home") {
                popUpTo("welcome") { inclusive = true }
            }
            viewModel.clearNavigationFlag() // reset to avoid re-trigger
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
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

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text(text = stringResource(R.string.email)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text(text = stringResource(R.string.passowrd)) },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = onLoginClick) {
            Text(text = stringResource(R.string.login_button))
        }

        TextButton(onClick = onForgotPasswordClick) {
            Text(text = stringResource(R.string.forgot_password))
        }
    }
}
