package com.example.rievent

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rievent.ui.login.LoginScreen
import com.example.rievent.ui.login.LoginViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.ui.allevents.AllEventsScreen
import com.example.rievent.ui.home.HomeScreen
import com.example.rievent.ui.welcome.WelcomeScreen
import com.example.rievent.ui.register.RegisterScreen
import com.example.rievent.ui.register.RegisterViewModel
import com.example.rievent.ui.welcome.WelcomeViewModel
import com.example.rievent.ui.createevent.CreateEventScreen
import com.example.rievent.ui.createevent.CreateEventViewModel
import com.example.rievent.ui.myevents.MyEventsScreen
import com.example.rievent.ui.myevents.MyEventsViewModel
import com.example.rievent.ui.singleevent.SingleEventScreen
import com.example.rievent.ui.updateevent.UpdateEventScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RiEventAppUI() {
    val navController = rememberNavController()
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "welcome"
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            val loginViewModel: LoginViewModel = viewModel()
            val uiState by loginViewModel.uiState.collectAsState()
            LoginScreen(
                state = uiState,
                onEmailChange = loginViewModel::onEmailChange,
                onPasswordChange = loginViewModel::onPasswordChange,
                onLoginClick = loginViewModel::onLoginClick,
                onForgotPasswordClick = loginViewModel::onForgotPasswordClick,
                viewModel = loginViewModel,
                navController = navController
            )
        }
        composable("register") {
            val registerViewModel: RegisterViewModel = viewModel()
            val uiState by registerViewModel.uiState.collectAsState()
            RegisterScreen(
                state = uiState,
                onEmailChange = registerViewModel::onEmailChange,
                onPasswordChange = registerViewModel::onPasswordChange,
                onConfirmPasswordChange = registerViewModel::onConfirmPasswordChange,
                onFirstNameChange = registerViewModel::onFirstNameChange,
                onLastNameChange = registerViewModel::onLastNameChange,
                onPhoneNumberChange = registerViewModel::onPhoneNumberChange,
                onDateOfBirthChange = registerViewModel::onDateOfBirthChange,
                onGenderChange = registerViewModel::onGenderChange,
                onTermsAndConditionsChange = registerViewModel::onTermsAndConditionsChange,
                onPrivacyPolicyChange = registerViewModel::onPrivacyPolicyChange,
                onRegisterClick = registerViewModel::onRegisterClick
            )
        }
        composable("welcome"){
            val welcomeViewModel = WelcomeViewModel(LocalContext.current)
            val uiState by welcomeViewModel.uiState.collectAsState()
            WelcomeScreen(
                onLoginClick = { navController.navigate("login") },
                onRegisterClick = { navController.navigate("register") },

                viewModel = welcomeViewModel,

                navController = navController
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("welcome") },
                onNavigateToProfile = { /* navController.navigate("profile") if you have one */ },
                onNavigateToEvents = {  navController.navigate("events")},
                onNavigateToCreateEvent = { navController.navigate("createEvent") },
                onNavigateToMyEvents = {navController.navigate("myEvents")}
            )
        }
        composable("createEvent") {
            val viewModel: CreateEventViewModel = viewModel()
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            CreateEventScreen(
                viewModel = viewModel,
                currentUserId = currentUserId,
                onCreated = { navController.navigate("home") },
                onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("welcome") },
                onNavigateToProfile = { /* navController.navigate("profile") if you have one */ },
                onNavigateToEvents = {  navController.navigate("events")},
                onNavigateToCreateEvent = { navController.navigate("createEvent") },
                onNavigateToMyEvents = {navController.navigate("myEvents")}

            )
        }
        composable("myEvents") {
            val viewModel: MyEventsViewModel = viewModel()

            MyEventsScreen(
                viewModel = viewModel,
                navController = navController,
                onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("welcome") },
                onNavigateToProfile = { /* navController.navigate("profile") if you have one */ },
                onNavigateToEvents = {  navController.navigate("events")},
                onNavigateToCreateEvent = { navController.navigate("createEvent") },
                onNavigateToMyEvents = {navController.navigate("myEvents")}
            )
        }
        composable("updateEvent/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
            UpdateEventScreen(
                eventId = eventId,
                onUpdated = { navController.navigate("myEvents") }
            )
        }
        composable("events") {
            AllEventsScreen(
                onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("welcome") },
                onNavigateToProfile = { /* navController.navigate("profile") if you have one */ },
                onNavigateToEvents = {  navController.navigate("events")},
                onNavigateToCreateEvent = { navController.navigate("createEvent") },
                onNavigateToMyEvents = {navController.navigate("myEvents")},
                onNavigateToSingleEvent = { eventId ->
                    navController.navigate("singleEvent/$eventId")
                }
            )
        }
        composable("singleEvent/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            if (eventId != null) {
                SingleEventScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() }
                )
            } else {
                // Handle error: eventId not found
                Text("Error: Event ID missing.")
            }
        }
    }
}
