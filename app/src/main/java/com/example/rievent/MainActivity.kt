package com.example.rievent


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

import com.example.rievent.ui.welcome.WelcomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*val result = FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase initialized? ${result != null}")*/




        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val welcomeViewModel = WelcomeViewModel(context)
            RiEventAppUI()
        }

            /*LoginScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onLoginClick = viewModel::onLoginClick,
                onForgotPasswordClick = viewModel::onForgotPasswordClick
            )*/
           /* RegisterScreen(
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
