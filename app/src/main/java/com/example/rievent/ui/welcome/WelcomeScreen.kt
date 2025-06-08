package com.example.rievent.ui.welcome

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    navController: NavController,
    viewModel: WelcomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val context = LocalContext.current

    val legacySignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val idToken = account.idToken
                if (idToken != null) {
                    viewModel.signInWithIdToken(idToken)
                } else {
                    Log.w("GoogleAuth", "Legacy sign-in result did not contain an ID token.")
                    viewModel.onLegacySignInFailed("Google token was null.")
                }
            } catch (e: ApiException) {
                Log.e("GoogleAuth", "Legacy Google sign-in failed", e)
                viewModel.onLegacySignInFailed("Google Sign-In failed: ${e.statusCode}")
            }
        } else {
            Log.w("GoogleAuth", "Legacy sign-in flow was cancelled by user.")
            viewModel.onLegacySignInFailed("Google Sign-In was cancelled.")
        }
    }

    LaunchedEffect(uiState.launchLegacyGoogleSignIn) {
        if (uiState.launchLegacyGoogleSignIn) {
            legacySignInLauncher.launch(viewModel.googleSignInClient.signInIntent)
            viewModel.onLegacySignInLaunched()
        }
    }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            navController.navigate("home") {
                popUpTo("welcome") { inclusive = true }
            }
            viewModel.onLoginSuccessNavigationConsumed()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(id = R.string.welcome_menu_title),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.welcome_home_button)) },
                    selected = false,
                    onClick = { navController.navigate("home") }
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.login_button)) },
                    selected = false,
                    onClick = onLoginClick
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(id = R.string.register_button)) },
                    selected = false,
                    onClick = onRegisterClick
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ri_event_logo),
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.home_text),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.login_button))
                    }
                    Button(onClick = onRegisterClick, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.register_button))
                    }
                    Button(
                        onClick = { viewModel.signInWithGoogle() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.welcome_google_login_button))
                    }
                }
            }

            uiState.error?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Text(
                text = stringResource(id = R.string.welcome_app_description),
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 250.dp)
            )
        }
    }
}