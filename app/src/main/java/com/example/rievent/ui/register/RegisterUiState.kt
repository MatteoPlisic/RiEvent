package com.example.rievent.ui.register

import android.net.Uri

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val gender: Boolean = true, // true for Male
    val termsAndConditions: Boolean = false,
    val privacyPolicy: Boolean = false,

    val profileImageUri: Uri? = null,

    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val phoneNumberError: String? = null,
    val dateOfBirthError: String? = null,
    val genderError: String? = null,
    val termsAndConditionsError: String? = null,
    val privacyPolicyError: String? = null,
    val profileImageError: String? = null,

    val success: Boolean = false,
    val isLoading: Boolean = false
)