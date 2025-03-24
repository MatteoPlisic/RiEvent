package com.example.rievent.ui.register

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val dateOfBirth: String = "",
    val gender: Boolean = false,
    val termsAndConditions: Boolean = false,
    val privacyPolicy: Boolean = false,
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
    val success: Boolean = false,
    //val availableCities: List<City> = emptyList()

)