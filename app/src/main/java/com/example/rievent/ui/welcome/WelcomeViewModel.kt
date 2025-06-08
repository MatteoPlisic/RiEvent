package com.example.rievent.ui.welcome

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.R
import com.example.rievent.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class WelcomeViewModel(private val context: Context) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun onLoginSuccessNavigationConsumed() {
        _uiState.update { it.copy(success = false) }
    }

    fun onLegacySignInLaunched() {
        _uiState.update { it.copy(launchLegacyGoogleSignIn = false) }
    }

    fun onLegacySignInFailed(errorMessage: String) {
        _uiState.update { it.copy(isLoading = false, error = errorMessage) }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = sha256(rawNonce)

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                signInToFirebaseWithIdToken(googleIdTokenCredential.idToken)

            } catch (e: GetCredentialException) {
                Log.w("GoogleAuth", "Credential Manager failed. Triggering legacy flow.")
                _uiState.update { it.copy(isLoading = false, launchLegacyGoogleSignIn = true) }
            } catch (e: Exception) {
                Log.e("GoogleAuth", "A non-credential error occurred", e)
                _uiState.update { it.copy(isLoading = false, error = "An unexpected error occurred.") }
            }
        }
    }

    fun signInWithIdToken(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                signInToFirebaseWithIdToken(idToken)
            } catch (e: Exception) {
                Log.e("GoogleAuth", "Sign-in with ID token from legacy flow failed", e)
                _uiState.update { it.copy(isLoading = false, error = "Sign-in failed: ${e.localizedMessage}") }
            }
        }
    }

    private suspend fun signInToFirebaseWithIdToken(idToken: String) {
        val firebaseAuthCredential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(firebaseAuthCredential).await()
        val firebaseUser = authResult.user

        if (firebaseUser != null) {
            Log.d("FirebaseAuth", "Signed in as: ${firebaseUser.displayName}")
            ensureUserProfileDocumentExists(firebaseUser)
            _uiState.update { it.copy(isLoading = false, success = true, error = null) }
        } else {
            Log.e("FirebaseAuth", "Firebase sign-in succeeded but user is null")
            _uiState.update { it.copy(isLoading = false, error = "Firebase sign-in failed.") }
        }
    }

    private suspend fun ensureUserProfileDocumentExists(firebaseUser: FirebaseUser) {
        val userDocRef = firestore.collection("users").document(firebaseUser.uid)
        try {
            val docSnapshot = userDocRef.get().await()
            if (!docSnapshot.exists()) {
                val newUserProfile = User(
                    id = firebaseUser.uid,
                    displayName = firebaseUser.displayName,
                    email = firebaseUser.email,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    createdAt = Timestamp.now(),
                    bio = null
                )
                userDocRef.set(newUserProfile).await()
            } else {
                val updates = mutableMapOf<String, Any?>()
                val existingUser = docSnapshot.toObject(User::class.java)
                if (existingUser?.displayName != firebaseUser.displayName && firebaseUser.displayName != null) {
                    updates["displayName"] = firebaseUser.displayName
                }
                if (existingUser?.photoUrl != firebaseUser.photoUrl?.toString()) {
                    updates["photoUrl"] = firebaseUser.photoUrl?.toString()
                }
                if (updates.isNotEmpty()) {
                    userDocRef.update(updates).await()
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreProfile", "Error ensuring user profile document", e)
        }
    }

    private fun sha256(input: String): String {
        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, item -> str + "%02x".format(item) }
    }
}