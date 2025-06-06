package com.example.rievent

import ChatListScreen
import ChatScreen
import ChatViewModel

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.rievent.ui.allevents.AllEventsScreen
import com.example.rievent.ui.allevents.AllEventsViewModel
import com.example.rievent.ui.chat.SearchUserScreen
import com.example.rievent.ui.chat.SearchUserViewModel
import com.example.rievent.ui.createevent.CreateEventScreen
import com.example.rievent.ui.home.HomeScreen
import com.example.rievent.ui.login.LoginScreen
import com.example.rievent.ui.login.LoginViewModel
import com.example.rievent.ui.map.MapScreen
import com.example.rievent.ui.map.MapViewModel
import com.example.rievent.ui.myevents.MyEventsScreen
import com.example.rievent.ui.myevents.MyEventsViewModel
import com.example.rievent.ui.register.RegisterScreen
import com.example.rievent.ui.register.RegisterViewModel
import com.example.rievent.ui.singleevent.SingleEventScreen
import com.example.rievent.ui.updateevent.UpdateEventScreen
import com.example.rievent.ui.userprofile.UserProfileScreen
import com.example.rievent.ui.userprofile.UserProfileViewModel
import com.example.rievent.ui.welcome.WelcomeScreen
import com.example.rievent.ui.welcome.WelcomeViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun RiEventAppUI(
    deepLinkEventIdFlow: StateFlow<String?>,
    onDeepLinkHandled: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "welcome"
    }

    val eventIdToNavigate by deepLinkEventIdFlow.collectAsState()

    val sharedChatViewModel: ChatViewModel = viewModel()



    LaunchedEffect(eventIdToNavigate) {
        Log.d("RiEventAppUI", "Deep link navigation NOT triggered for eventId: $eventIdToNavigate")
        if (eventIdToNavigate != null) {
            Log.d("RiEventAppUI", "Deep link navigation triggered for eventId: $eventIdToNavigate")
            navController.navigate("singleEvent/$eventIdToNavigate") {
                // Optional: Add NavOptions like popUpTo to clear back stack if needed
                // launchSingleTop = true // If navigating to same screen type
            }
            onDeepLinkHandled() // Signal that the deep link has been processed
        }
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

            LaunchedEffect(key1 = registerViewModel, key2 = navController) {
                registerViewModel.navigateToHome.collectLatest {
                    Log.d("NavGraph_Register", "navigateToHome from Register collected. Navigating to home.")
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true } // Clear register screen
                        popUpTo("welcome") { inclusive = true } // Also clear welcome if it was before register
                        launchSingleTop = true
                    }
                }
            }

            RegisterScreen(
                navController = navController,
                viewModel = registerViewModel
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
                navController = navController
            )
        }
        composable("createEvent") {

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

            CreateEventScreen(

                currentUserId = currentUserId,
                onCreated = { navController.navigate("home") },
                onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("welcome") },
                navController = navController,
            )
        }
        composable("myEvents") {
            val viewModel: MyEventsViewModel = viewModel()

            MyEventsScreen(
                viewModel = viewModel,
                navController = navController,
                onLogout = { FirebaseAuth.getInstance().signOut(); navController.navigate("welcome") },
                onNavigateToProfile = { navController.navigate("myprofile")  },
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
,                navController = navController
            )
        }
        composable("events") {
            AllEventsScreen(
                navController = navController

            )
        }
        composable("singleEvent/{eventId}") { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            if (eventId != null) {
                SingleEventScreen(
                    eventId = eventId,
                    onBack = { navController.popBackStack() },
                    onNavigateToUserProfile = { userId -> navController.navigate("profile/$userId") }
                )
            } else {

                Text("Error: Event ID missing.")
            }
        }
        composable("profile/{userId}") { backStackEntry -> 
            val userId = backStackEntry.arguments?.getString("userId")
            if(userId != null) {
                val viewModel: UserProfileViewModel = viewModel()
                val allEventsViewModel: AllEventsViewModel = viewModel()
                UserProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onNavigateToSingleEvent = { eventId ->
                        navController.navigate("singleEvent/$eventId")
                    },
                    viewModel = viewModel,
                    allEventsViewModel = allEventsViewModel,
                    isCurrentUserProfile = userId == FirebaseAuth.getInstance().currentUser?.uid,
                    chatViewModel = sharedChatViewModel,
                    navController = navController
                )
            }
            else{
                Text("Error: User ID missing.")
            }
        }
        composable("myprofile") { backStackEntry ->
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if(userId != null) {
                val viewModel: UserProfileViewModel = viewModel()
                val allEventsViewModel: AllEventsViewModel = viewModel()
                UserProfileScreen(
                    userId = userId,
                    viewModel = viewModel,
                    allEventsViewModel = allEventsViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToSingleEvent = { eventId ->
                        navController.navigate("singleEvent/$eventId")
                    },
                    isCurrentUserProfile = userId == FirebaseAuth.getInstance().currentUser?.uid,
                    chatViewModel = sharedChatViewModel,
                    navController = navController
                )
            }
            else{
                Text("Error: User ID missing.")
            }
        }
        composable("eventMap") { backStackEntry ->
            val viewModel: MapViewModel = viewModel()
            MapScreen(
                navController = navController,
                viewModel = viewModel,
            )
        }
        composable("messages") {

            val viewModel: ChatViewModel = viewModel() // Shared ViewModel for chat
            ChatListScreen(
                navController = navController,
                viewModel = sharedChatViewModel // Pass the shared ChatViewModel
            )
        }

        composable("conversation/{chatId}") { backStackEntry ->

            val chatId = backStackEntry.arguments?.getString("chatId")
            if (chatId != null) {
                ChatScreen(
                    chatId = chatId,
                    navController = navController,
                    viewModel = sharedChatViewModel
                )
            } else {
                Text("Error: Chat ID missing.")
            }
        }

        composable("searchUsers") {
            val viewModel: SearchUserViewModel = viewModel()
            SearchUserScreen(
                navController = navController,
                findUserViewModel = viewModel,
                chatViewModel = sharedChatViewModel
            )
        }
    }
}
