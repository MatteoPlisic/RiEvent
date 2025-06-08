package com.example.rievent


import ChatScreen

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
import com.example.rievent.ui.chat.ChatListScreen
import com.example.rievent.ui.chat.ChatViewModel
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
    // MODIFIED: Updated signature to accept separate chat/event flows and handlers
    deepLinkEventIdFlow: StateFlow<String?>,
    deepLinkChatIdFlow: StateFlow<String?>,
    onEventDeepLinkHandled: () -> Unit,
    onChatDeepLinkHandled: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        "home"
    } else {
        "welcome"
    }

    // This ViewModel is shared across several chat-related screens
    val sharedChatViewModel: ChatViewModel = viewModel()

    // --- DEEP LINK HANDLING ---

    // Listener for EVENT deep links
    val eventIdToNavigate by deepLinkEventIdFlow.collectAsState()
    LaunchedEffect(eventIdToNavigate) {
        if (eventIdToNavigate != null) {
            Log.d("DeepLinkUI", "Event deep link triggered. Navigating to event: $eventIdToNavigate")
            navController.navigate("singleEvent/$eventIdToNavigate")
            onEventDeepLinkHandled() // MODIFIED: Call the correct handler
        }
    }

    // Listener for CHAT deep links
    val chatIdToNavigate by deepLinkChatIdFlow.collectAsState()
    LaunchedEffect(chatIdToNavigate) {
        if (chatIdToNavigate != null) {
            Log.d("DeepLinkUI", "Chat deep link triggered. Navigating to conversation: $chatIdToNavigate")
            // FIXED: Use the correct navigation route "conversation/{chatId}"
            navController.navigate("conversation/$chatIdToNavigate")
            onChatDeepLinkHandled() // MODIFIED: Call the correct handler
        }
    }


    // --- NAVIGATION GRAPH ---

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
                navController = navController,
                onBack = navController::popBackStack
            )
        }
        composable("register") {
            val registerViewModel: RegisterViewModel = viewModel()
            LaunchedEffect(key1 = registerViewModel, key2 = navController) {
                registerViewModel.navigateToHome.collectLatest {
                    Log.d("NavGraph_Register", "navigateToHome from Register collected. Navigating to home.")
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                        popUpTo("welcome") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            RegisterScreen(
                navController = navController,
                viewModel = registerViewModel,
                onBackClick = navController::popBackStack
            )
        }
        composable("welcome"){
            val welcomeViewModel = WelcomeViewModel(LocalContext.current)
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
                onCreated = { navController.navigate("home") },
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
                onUpdated = { navController.navigate("myEvents") },
                navController = navController
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
                    onNavigateToUserProfile = { userId -> navController.navigate("profile/$userId") },
                    navController = navController
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
        composable("myprofile") {
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
                    isCurrentUserProfile = true,
                    chatViewModel = sharedChatViewModel,
                    navController = navController
                )
            }
            else{
                Text("Error: User ID missing.")
            }
        }
        composable("eventMap") {
            val viewModel: MapViewModel = viewModel()
            MapScreen(
                navController = navController,
                viewModel = viewModel,
            )
        }
        composable("messages") {
            ChatListScreen(
                navController = navController,
                viewModel = sharedChatViewModel
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