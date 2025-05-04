package com.example.rievent.ui.welcome


import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID


class WelcomeViewModel(private val context: Context):ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState
    private val _navigateToHome = MutableSharedFlow<Unit>(replay = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome

    fun clearNavigationFlag() {
        _uiState.update { it.copy(success = false) }
    }

    private val credentialManager = CredentialManager.create(context)

    fun requestGoogleIdToken() {
        viewModelScope.launch {
            try {

                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }


                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setNonce(hashedNonce)
                    .build()


                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()


                val result = credentialManager.getCredential(context, request)
                val credential = result.credential


                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                Log.d("GoogleAuth", "✅ Google ID Token: $idToken")


                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                    .addOnSuccessListener { result ->
                        val user = result.user
                        Log.d("FirebaseAuth", "✅ Signed in as: ${user?.displayName}")
                        //Toast.makeText(context, "Signed in as: ${user?.displayName}", Toast.LENGTH_SHORT).show()
                        _uiState.update { it.copy(success = true) }
                        viewModelScope.launch {
                            _navigateToHome.emit(Unit)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseAuth", "❌ Firebase sign-in failed", exception)

                    }


            } catch (e: Exception) {
                Log.e("GoogleAuth", "❌ Google Sign-In failed", e)

            }
        }
    }
}
