package com.example.rievent.ui.welcome

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.R
import com.example.rievent.models.User // Import your User data class
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp // Import Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // Import FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore // Import Firestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Import for asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // For awaiting Firebase tasks
import java.security.MessageDigest
import java.util.UUID



class WelcomeViewModel(private val context: Context) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val credentialManager = CredentialManager.create(context)

    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()


    private val _navigateToHome = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome


    fun clearNavigationFlag() {

        _uiState.update { it.copy(success = false, error = null) }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {

                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, item -> str + "%02x".format(item) }

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


                val firebaseAuthCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(firebaseAuthCredential).await() // Use await
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    Log.d("FirebaseAuth", "✅ Signed in as: ${firebaseUser.displayName}")

                    ensureUserProfileDocumentExists(firebaseUser)

                    _uiState.update { it.copy(isLoading = false, success = true, error = null) }
                    _navigateToHome.emit(Unit)
                } else {
                    Log.e("FirebaseAuth", "❌ Firebase sign-in succeeded but user is null")
                    _uiState.update { it.copy(isLoading = false, success = false, error = "Firebase sign-in failed: User data not found.") }
                }

            } catch (e: Exception) {
                Log.e("GoogleAuth", "❌ Google Sign-In or Firebase process failed", e)
                _uiState.update { it.copy(isLoading = false, success = false, error = "Google Sign-In failed: ${e.localizedMessage}") }
            }
        }
    }


    private suspend fun ensureUserProfileDocumentExists(firebaseUser: FirebaseUser) {
        val userDocRef = firestore.collection("users").document(firebaseUser.uid)
        Log.d("FirestoreProfile", "ℹ️ Checking Firestore user profile for ${firebaseUser.uid}")
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
                Log.d("FirestoreProfile", "✅ Created Firestore user profile for ${firebaseUser.uid}")
            } else {
                Log.d("FirestoreProfile", "ℹ️ Firestore user profile already exists for ${firebaseUser.uid}")

                val updates = mutableMapOf<String, Any?>()
                val existingUser = docSnapshot.toObject(User::class.java)

                if (existingUser?.displayName != firebaseUser.displayName && firebaseUser.displayName != null) {
                    updates["displayName"] = firebaseUser.displayName
                }
                val newPhotoUrl = firebaseUser.photoUrl?.toString()
                if (existingUser?.photoUrl != newPhotoUrl) {
                    updates["photoUrl"] = newPhotoUrl
                }

                if (updates.isNotEmpty()) {
                    userDocRef.update(updates).await()
                    Log.d("FirestoreProfile", "🔄 Updated Firestore user profile for ${firebaseUser.uid} with new Auth data.")
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreProfile", "❌ Error ensuring/updating user profile document for ${firebaseUser.uid}: ${e.message}", e)

        }
    }
}