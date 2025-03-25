package com.example.rievent

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.ui.login.LoginScreen
import com.example.rievent.ui.login.LoginViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase initialized? ${result != null}")
        enableEdgeToEdge()
        setContent {
            //val viewModel: RegisterViewModel = viewModel()
            val viewModel: LoginViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()

            LoginScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLoginClick = viewModel::onLoginClick,
                onForgotPasswordClick = viewModel::onForgotPasswordClick
            )
            /*RegisterScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onFirstNameChange = viewModel::onFirstNameChange,
                onLastNameChange = viewModel::onLastNameChange,
                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                onDateOfBirthChange = viewModel::onDateOfBirthChange,
                onGenderChange = viewModel::onGenderChange,
                onTermsAndConditionsChange = viewModel::onTermsAndConditionsChange,
                onPrivacyPolicyChange = viewModel::onPrivacyPolicyChange,
                onRegisterClick = viewModel::onRegisterClick,


            )*/
        }
    }
}
