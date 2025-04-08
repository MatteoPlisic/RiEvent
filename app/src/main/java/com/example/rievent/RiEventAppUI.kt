package com.example.rievent

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rievent.ui.login.LoginScreen

import com.example.rievent.ui.login.LoginViewModel

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.ui.home.HomeScreen
import com.example.rievent.ui.welcome.WelcomeScreen
import com.example.rievent.ui.register.RegisterScreen
import com.example.rievent.ui.register.RegisterViewModel
import com.example.rievent.ui.welcome.WelcomeViewModel

@Composable
fun RiEventAppUI(onGoogleLoginClick: () -> Unit) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "welcome") {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            val uiState by loginViewModel.uiState.collectAsState()
            LoginScreen(
                state = uiState,
                onEmailChange = loginViewModel::onEmailChange,
                onPasswordChange = loginViewModel::onPasswordChange,
                onLoginClick = loginViewModel::onLoginClick,
                onForgotPasswordClick = loginViewModel::onForgotPasswordClick,
                viewModel = loginViewModel,
                navController = navController
            )
        }
        composable("register") {
            val registerViewModel: RegisterViewModel = viewModel()
            val uiState by registerViewModel.uiState.collectAsState()
            RegisterScreen(
                state = uiState,
                onEmailChange = registerViewModel::onEmailChange,
                onPasswordChange = registerViewModel::onPasswordChange,
                onConfirmPasswordChange = registerViewModel::onConfirmPasswordChange,
                onFirstNameChange = registerViewModel::onFirstNameChange,
                onLastNameChange = registerViewModel::onLastNameChange,
                onPhoneNumberChange = registerViewModel::onPhoneNumberChange,
                onDateOfBirthChange = registerViewModel::onDateOfBirthChange,
                onGenderChange = registerViewModel::onGenderChange,
                onTermsAndConditionsChange = registerViewModel::onTermsAndConditionsChange,
                onPrivacyPolicyChange = registerViewModel::onPrivacyPolicyChange,
                onRegisterClick = registerViewModel::onRegisterClick
            )
        }
        composable("welcome"){
            WelcomeScreen(
                onLoginClick = { navController.navigate("login") },
                onRegisterClick = { navController.navigate("register") },
                onGoogleLoginClick = onGoogleLoginClick,
                viewModel = WelcomeViewModel(),
                navController = navController
            )
        }
        composable("home") {
            HomeScreen()
        }
    }
}
