package com.example.rievent.ui.register

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.rievent.R
import com.example.rievent.ui.utils.DatePickerField
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: RegisterViewModel,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val state by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onProfileImageChange(uri)
    }

    LaunchedEffect(key1 = viewModel) {
        viewModel.navigateToHome.collectLatest {
            Log.d("RegisterScreen", "Navigating to home route.")
            navController.navigate("home") {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back_button_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // A spacer to push content down a bit from the TopAppBar
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.ri_event_logo),
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier.size(120.dp)
            )

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        1.dp,
                        if (state.profileImageError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                    .clickable { imagePickerLauncher.launch("image/*") }
            ) {
                if (state.profileImageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = state.profileImageUri),
                        contentDescription = stringResource(id = R.string.register_profile_picture_preview),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(id = R.string.register_default_profile_picture),
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.AddAPhoto,
                    contentDescription = stringResource(id = R.string.register_add_photo),
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).background(Color.Black.copy(alpha=0.3f), CircleShape).padding(4.dp)
                )
            }
            state.profileImageError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text(stringResource(id = R.string.register_first_name_label)) },
                isError = state.firstNameError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.firstNameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChange,
                label = { Text(stringResource(id = R.string.register_last_name_label)) },
                isError = state.lastNameError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.lastNameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text(stringResource(id = R.string.email)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = state.emailError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.emailError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text(stringResource(id = R.string.register_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                isError = state.passwordError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text(stringResource(id = R.string.register_confirm_password_label)) },
                visualTransformation = PasswordVisualTransformation(),
                isError = state.confirmPasswordError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.confirmPasswordError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(
                label = stringResource(id = R.string.register_dob_label),
                value = state.dateOfBirth,
                onDateSelected = viewModel::onDateOfBirthChange,
                onTextChange = viewModel::onDateOfBirthChange
            )
            state.dateOfBirthError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = viewModel::onPhoneNumberChange,
                label = { Text(stringResource(id = R.string.register_phone_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                isError = state.phoneNumberError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            state.phoneNumberError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = R.string.register_gender_label), style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(id = R.string.register_gender_male), style = MaterialTheme.typography.bodyMedium)
                RadioButton(
                    selected = state.gender,
                    onClick = { viewModel.onGenderChange(true) }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(stringResource(id = R.string.register_gender_female), style = MaterialTheme.typography.bodyMedium)
                RadioButton(
                    selected = !state.gender,
                    onClick = { viewModel.onGenderChange(false) }
                )
            }
            state.genderError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.termsAndConditions,
                        onValueChange = viewModel::onTermsAndConditionsChange
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.termsAndConditions,
                    onCheckedChange = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.register_terms_and_conditions), style = MaterialTheme.typography.bodyMedium)
            }
            state.termsAndConditionsError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.privacyPolicy,
                        onValueChange = viewModel::onPrivacyPolicyChange
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.privacyPolicy,
                    onCheckedChange = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(id = R.string.register_privacy_policy), style = MaterialTheme.typography.bodyMedium)
            }
            state.privacyPolicyError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = viewModel::onRegisterClick,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(id = R.string.register_create_account_button), style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}