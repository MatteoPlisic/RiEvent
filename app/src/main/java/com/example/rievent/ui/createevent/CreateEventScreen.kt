package com.example.rievent.ui.createevent

import Event
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.rievent.ui.utils.DatePickerField
import com.example.rievent.ui.utils.Drawer
import com.example.rievent.ui.utils.TimePickerField
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel,
    onCreated: () -> Unit,
    currentUserId: String,
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToEvents: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToMyEvents: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()


    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var ownerName by remember { mutableStateOf("") }

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")

    // Trigger on success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.resetState()
            onCreated()
        }
    }
    Drawer(
        title = "Home",
        onLogout = onLogout,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToMyEvents = onNavigateToMyEvents,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 50.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                item{TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })}
                item{TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") })}


                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        category = option
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Start Date
                item{DatePickerField(
                    label = "Start Date",
                    value = startDate,
                    onDateSelected = { startDate = it },
                    onTextChange = { startDate = it }
                )}

                // Start Time
                item{TimePickerField(
                    label = "Start Time",
                    value = startTime,
                    onTimeSelected = { startTime = it },
                    onTextChange = { startTime = it }
                )}

                // End Date
                item{DatePickerField(
                    label = "End Date",
                    value = endDate,
                    onDateSelected = { endDate = it },
                    onTextChange = { endDate = it }
                )}

                // End Time
                item{TimePickerField(
                    label = "End Time",
                    value = endTime,
                    onTimeSelected = { endTime = it },
                    onTextChange = { endTime = it }
                )}

                item{TextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") })}
                item{TextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") })}
                item{TextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") })}

                item{Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Public Event")
                }}

                item{Button(
                    onClick = {
                        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        val startTimestamp = runCatching {
                            Timestamp(formatter.parse("$startDate $startTime")!!)
                        }.getOrNull()
                        val endTimestamp = runCatching {
                            Timestamp(formatter.parse("$endDate $endTime")!!)
                        }.getOrNull()

                        val geoPoint = if (latitude.isNotBlank() && longitude.isNotBlank()) {
                            GeoPoint(latitude.toDouble(), longitude.toDouble())
                        } else null

                        val db = FirebaseFirestore.getInstance()
                        val ownerName = FirebaseAuth.getInstance().currentUser?.displayName ?: ""



                                val event = Event(
                                    name = name,
                                    description = description,
                                    category = category,
                                    ownerId = currentUserId,
                                    startTime = startTimestamp,
                                    endTime = endTimestamp,
                                    address = address,
                                    location = geoPoint,
                                    isPublic = isPublic,
                                    ownerName = ownerName
                                )

                                viewModel.createEvent(event)

                    }
                    //enabled = !isLoading
                ) {
                    Text(if (isLoading) "Creating..." else "Create Event")
                }

                if (errorMessage != null) {
                    Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
            }}

        }
    }
}
