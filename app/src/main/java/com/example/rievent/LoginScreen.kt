package com.example.rievent

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
@Preview
fun LoginScreen(){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painter = painterResource(id = R.drawable.ri_event_logo), contentDescription = "RiEvent Logo",
        modifier = Modifier.size(200.dp))

        Text(text = "Welcome back", fontSize = 30.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(10.dp))

        Text(text = "Login to your account", fontSize = 15.sp)

        OutlinedTextField(value = email, onValueChange = {email = it},label = { Text(text = "Email address")})

        Spacer(modifier = Modifier.height(10.dp))


        OutlinedTextField(value = password, onValueChange = {password = it},label = { Text(text = "Password")},
            visualTransformation = PasswordVisualTransformation())

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { }){
            Text(text = "Login")
        }
        
        TextButton(onClick = { }) {
            Text(text = "Forgot password?")
        }

    }

}