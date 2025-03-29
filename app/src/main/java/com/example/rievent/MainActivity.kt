package com.example.rievent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.ui.register.RegisterViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*val result = FirebaseApp.initializeApp(this)
        Log.d("FirebaseTest", "Firebase initialized? ${result != null}")*/
        lateinit var auth: FirebaseAuth
        lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
        auth = FirebaseAuth.getInstance()

        val googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val user = auth.currentUser
                            Log.d("GoogleLogin", "Login success: ${user?.email}")

                        } else {
                            Log.e("GoogleLogin", "Firebase login failed", signInTask.exception)
                        }
                    }
            } catch (e: ApiException) {
                Log.e("GoogleLogin", "Google sign-in failed", e)
            }
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: RegisterViewModel = viewModel()
            //val viewModel: LoginViewModel = viewModel()
            val state by viewModel.uiState.collectAsState()
            setContent {
                RiEventAppUI(
                    onGoogleLoginClick = {
                        val signInIntent = googleSignInClient.signInIntent
                        googleSignInLauncher.launch(signInIntent)
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
}
