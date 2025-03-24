package com.example.rievent.ui.register
import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rievent.R
import java.util.Date


@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onGenderChange: (Boolean) -> Unit,
    onTermsAndConditionsChange: (Boolean) -> Unit,
    onPrivacyPolicyChange: (Boolean) -> Unit,
    onRegisterClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Image(
            painter = painterResource(id = R.drawable.ri_event_logo),
            contentDescription = stringResource(R.string.logo),
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = stringResource(R.string.register_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.firstName,
            onValueChange = onFirstNameChange,
            label = { Text("First Name") },
            isError = state.firstNameError != null
        )
        state.firstNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.lastName,
            onValueChange = onLastNameChange,
            label = { Text("Last Name") },
            isError = state.lastNameError != null
        )
        state.lastNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError != null
        )
        state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = state.passwordError != null
        )
        state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = state.confirmPasswordError != null
        )
        state.confirmPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        DateOfBirthPickerField(
            dateText = state.dateOfBirth,
            onDateSelected = onDateOfBirthChange,
            onTextChange = onDateOfBirthChange,

        )

        state.dateOfBirthError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = state.phoneNumber,
            onValueChange = onPhoneNumberChange,
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            isError = state.phoneNumberError != null
        )
        state.phoneNumberError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 12.dp)
        ) {
            Text("Gender: ")
            Spacer(modifier = Modifier.width(12.dp))
            Text("Male")
            RadioButton(
                selected = state.gender,
                onClick = { onGenderChange(true) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Female")
            RadioButton(
                selected = !state.gender,
                onClick = { onGenderChange(false) }
            )
        }

        state.genderError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .toggleable(
                    value = state.termsAndConditions,
                    onValueChange = onTermsAndConditionsChange
                )
        ) {
            Checkbox(
                checked = state.termsAndConditions,
                onCheckedChange = null
            )
            Text("Accept Terms & Conditions")
        }
        state.termsAndConditionsError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .toggleable(
                    value = state.privacyPolicy,
                    onValueChange = onPrivacyPolicyChange
                )
        ) {
            Checkbox(
                checked = state.privacyPolicy,
                onCheckedChange = null
            )
            Text("Accept Privacy Policy")
        }
        state.privacyPolicyError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onRegisterClick) {
            Text("Register")
        }
    }
}

@SuppressLint("SimpleDateFormat")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateOfBirthPickerField(
    dateText: String,
    onDateSelected: (String) -> Unit,
    onTextChange: (String) -> Unit,
    error: String? = null
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatted = SimpleDateFormat("dd-MM-yyyy")
                            .format(Date(millis))
                        onDateSelected(formatted)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    OutlinedTextField(
        value = dateText,
        onValueChange = onTextChange,
        label = { Text("Date of Birth") },
        placeholder = { Text("DD-MM-YYYY") },
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    error?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}