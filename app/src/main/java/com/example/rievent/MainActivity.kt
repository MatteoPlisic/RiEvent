package com.example.rievent


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest

import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.auth.GoogleCredentialAuthManager
import com.example.rievent.ui.register.RegisterViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*val result = FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase initialized? ${result != null}")*/




        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            val googleAuthManager = GoogleCredentialAuthManager(context)
            RiEventAppUI(
                onGoogleLoginClick = {
                    coroutineScope.launch {

                        val idToken = googleAuthManager.requestGoogleIdToken()
                        if (idToken != null) {
                            Log.d("Auth", "Successfully received token: $idToken")
                            Toast.makeText(context, "You are logged in!", Toast.LENGTH_SHORT).show()

                        } else {
                            Toast.makeText(context, "Failed to log in", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
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
