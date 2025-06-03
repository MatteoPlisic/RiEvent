package com.example.rievent.ui.register

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    private val _navigateToHome = MutableSharedFlow<Unit>(replay = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome

    private val auth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore
    private val storage = Firebase.storage

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, firstNameError = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, lastNameError = null) }
    }

    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value, phoneNumberError = null) }
    }

    fun onDateOfBirthChange(value: String) {
        _uiState.update { it.copy(dateOfBirth = value, dateOfBirthError = null) }
    }

    fun onGenderChange(value: Boolean) {
        _uiState.update { it.copy(gender = value, genderError = null) }
    }

    fun onTermsAndConditionsChange(value: Boolean) {
        _uiState.update { it.copy(termsAndConditions = value, termsAndConditionsError = null) }
    }

    fun onPrivacyPolicyChange(value: Boolean) {
        _uiState.update { it.copy(privacyPolicy = value, privacyPolicyError = null) }
    }

    fun onProfileImageChange(uri: Uri?) {
        _uiState.update { it.copy(profileImageUri = uri, profileImageError = null) }
    }

    fun onRegisterClick() {
        val state = _uiState.value
        if (!validateInputs(state)) return

        _uiState.update { it.copy(isLoading = true) }

        auth.createUserWithEmailAndPassword(state.email, state.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser == null) {
                        _uiState.update { it.copy(isLoading = false, emailError = "User authentication failed unexpectedly.") }
                        return@addOnCompleteListener
                    }
                    val uid = firebaseUser.uid
                    processUserProfile(uid, state)
                } else {
                    val error = task.exception?.localizedMessage ?: "Registration failed. Please try again."
                    Log.e("RegisterVM", "Registration failed: $error", task.exception)
                    _uiState.update { it.copy(isLoading = false, emailError = error) }
                }
            }
    }

    private fun validateInputs(state: RegisterUiState): Boolean {
        var isValid = true
        _uiState.update { it.copy(
            emailError = null, passwordError = null, confirmPasswordError = null,
            firstNameError = null, lastNameError = null, termsAndConditionsError = null,
            privacyPolicyError = null, profileImageError = null
        )}

        if (state.firstName.isBlank()) {
            _uiState.update { it.copy(firstNameError = "First name is required") }
            isValid = false
        }
        if (state.lastName.isBlank()) {
            _uiState.update { it.copy(lastNameError = "Last name is required") }
            isValid = false
        }
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Enter a valid email") }
            isValid = false
        }
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }
        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }
        if (!state.termsAndConditions) {
            _uiState.update { it.copy(termsAndConditionsError = "You must accept the terms and conditions") }
            isValid = false
        }
        if (!state.privacyPolicy) {
            _uiState.update { it.copy(privacyPolicyError = "You must accept the privacy policy") }
            isValid = false
        }
        return isValid
    }

    private fun processUserProfile(uid: String, state: RegisterUiState) {
        viewModelScope.launch {
            var photoUrlString: String? = null
            if (state.profileImageUri != null) {
                try {
                    val imageRef = storage.reference.child("profile_images/$uid/${state.profileImageUri.lastPathSegment}")
                    val uploadTask = imageRef.putFile(state.profileImageUri).await()
                    photoUrlString = uploadTask.storage.downloadUrl.await().toString()
                    Log.d("RegisterVM", "Image uploaded: $photoUrlString")
                } catch (e: Exception) {
                    Log.e("RegisterVM", "Image upload failed", e)
                    _uiState.update { it.copy(profileImageError = "Profile image upload failed: ${e.localizedMessage}") }

                }
            }

            val displayName = "${state.firstName} ${state.lastName}".trim()
            val userData = User(
                uid = uid,
                displayName = if (displayName.isNotBlank()) displayName else "Guest user",
                email = state.email,
                photoUrl = photoUrlString ?: ""
            )

            try {
                firestore.collection("users").document(uid).set(userData).await()
                Log.d("RegisterVM", "User profile saved to Firestore.")
                _uiState.update { it.copy(isLoading = false, success = true) }
                _navigateToHome.tryEmit(Unit)
            } catch (e: Exception) {
                Log.e("RegisterVM", "Failed to save user profile: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, emailError = "Account created, but profile save failed: ${e.message}") }
            }
        }
    }
}