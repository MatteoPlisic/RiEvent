package com.example.rievent.ui.register

import androidx.lifecycle.ViewModel
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class RegisterViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState


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


        if (state.email.isBlank() || state.password.length < 6) {
            _uiState.update { it.copy(emailError = "Enter valid email and password (6+ characters)") }
            return
        }

        FirebaseAuth.getInstance()
            .createUserWithEmailAndPassword(state.email, state.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener


                    val userData = mapOf(
                        "firstName" to state.firstName,
                        "lastName" to state.lastName,
                        "email" to state.email,
                        "dateOfBirth" to state.dateOfBirth,
                        "phone" to state.phoneNumber,
                        "gender" to if (state.gender) "male" else "female"
                    )

                    // Save to Firestore
                    Firebase.firestore.collection("users").document(uid).set(userData)
                        .addOnSuccessListener {
                            _uiState.update { it.copy(success = true) }
                        }
                        .addOnFailureListener { e ->
                            _uiState.update { it.copy(emailError = "User created but failed to save profile: ${e.message}") }
                        }

                    navOptions {  }

                } else {
                    val error = task.exception?.localizedMessage ?: "Registration failed"
                    _uiState.update { it.copy(emailError = error) }
                }
            }
    }


}