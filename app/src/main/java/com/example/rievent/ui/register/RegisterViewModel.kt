package com.example.rievent.ui.register

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rievent.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    private val _navigateToHome = MutableSharedFlow<Unit>(replay = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome

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

    fun onRegisterClick() {
        val state = uiState.value

        // Basic client-side validation (you should add more comprehensive validation)
        var isValid = true
        if (state.firstName.isBlank()) {
            _uiState.update { it.copy(firstNameError = "First name cannot be empty") }
            isValid = false
        }
        if (state.lastName.isBlank()) {
            _uiState.update { it.copy(lastNameError = "Last name cannot be empty") }
            isValid = false
        }
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email)
                .matches()
        ) {
            _uiState.update { it.copy(emailError = "Enter a valid email address") }
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
        // Add other validations (phone number, DOB format if not using a proper picker that validates)

        if (!isValid) {
            return // Stop if validation fails
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                firstNameError = null,
                lastNameError = null,
                termsAndConditionsError = null,
                privacyPolicyError = null
            )
        } // Clear previous general errors

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(state.email, state.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = FirebaseAuth.getInstance().currentUser
                    if (firebaseUser == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                emailError = "User created but UID is missing."
                            )
                        }
                        return@addOnCompleteListener
                    }
                    val uid = firebaseUser.uid

                    val userData = User(
                        uid = uid,
                        displayName = "${state.firstName} ${state.lastName}".trim(),
                        email = state.email,
                        // You'll need to map other fields from RegisterUiState to your User model
                        // e.g., phoneNumber = state.phoneNumber, gender = if(state.gender) "Male" else "Female", etc.
                        // photoUrl = "", // Default or allow upload later
                        // bio = "",      // Default
                    )

                    Firebase.firestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            Log.d("RegisterVM", "User profile saved to Firestore.")

                            _uiState.update { it.copy(isLoading = false, success = true) }
                            viewModelScope.launch {
                                _navigateToHome.tryEmit(Unit) // Use tryEmit for SharedFlow with replay=1
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("RegisterVM", "Failed to save user profile: ${e.message}", e)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    emailError = "Account created but profile save failed: ${e.message}"
                                )
                            }
                        }
                } else {
                    val error =
                        task.exception?.localizedMessage ?: "Registration failed. Please try again."
                    Log.e("RegisterVM", "Registration failed: $error", task.exception)
                    _uiState.update { it.copy(isLoading = false, emailError = error) }
                }
            }
    }


}