package com.example.rievent.ui.createevent

import Event // Assuming Event data class is in the root package or correctly imported
// Remove android.icu.text.SimpleDateFormat if you are using java.text.SimpleDateFormat
// import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rievent.ui.utils.DatePickerField // Ensure this path is correct
import com.example.rievent.ui.utils.Drawer // Ensure this path is correct
import com.example.rievent.ui.utils.TimePickerField // Ensure this path is correct
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat // Import java.text.SimpleDateFormat
import java.util.* // For Locale and Calendar
import android.util.Log // For debugging

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel, // Assuming you have this ViewModel
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
    var startDate by remember { mutableStateOf("") } // Will be "yyyy-MM-dd"
    var startTime by remember { mutableStateOf("") } // Will be "HH:mm"
    var endDate by remember { mutableStateOf("") }   // Will be "yyyy-MM-dd"
    var endTime by remember { mutableStateOf("") }   // Will be "HH:mm"
    var address by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    // var ownerName by remember { mutableStateOf("") } // ownerName will be fetched from Auth

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            // Optionally clear fields here or let onCreated() handle navigation which recomposes
            name = ""
            description = ""
            category = ""
            startDate = ""
            startTime = ""
            endDate = ""
            endTime = ""
            address = ""
            latitude = ""
            longitude = ""
            isPublic = true
            // ...
            viewModel.resetState() // Important
            onCreated()
        }
    }

    Drawer(
        title = "Create Event", // Changed title for clarity
        onLogout = onLogout,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToMyEvents = onNavigateToMyEvents,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 16.dp, start = 16.dp, end = 16.dp), // Added horizontal padding
            contentAlignment = Alignment.TopCenter // Align content to top for scrolling
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp), // Reduced spacing a bit
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth() // Use full width within the padded Box
            ) {
                item { TextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
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

                item {
                    DatePickerField(
                        label = "Start Date",
                        value = startDate,
                        onDateSelected = { selectedDateString -> startDate = selectedDateString },
                        onTextChange = { text -> startDate = text } // Update state if manual edit is allowed
                    )
                }

                item {
                    TimePickerField(
                        label = "Start Time",
                        value = startTime,
                        onTimeSelected = { selectedTimeString -> startTime = selectedTimeString },
                        onTextChange = { text -> startTime = text } // Update state if manual edit is allowed
                    )
                }

                item {
                    DatePickerField(
                        label = "End Date",
                        value = endDate,
                        onDateSelected = { selectedDateString -> endDate = selectedDateString },
                        onTextChange = { text -> endDate = text }
                    )
                }

                item {
                    TimePickerField(
                        label = "End Time",
                        value = endTime,
                        onTimeSelected = { selectedTimeString -> endTime = selectedTimeString },
                        onTextChange = { text -> endTime = text }
                    )
                }

                item { TextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude (Optional)") }, modifier = Modifier.fillMaxWidth()) }
                item { TextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude (Optional)") }, modifier = Modifier.fillMaxWidth()) }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Make Event Public")
                    }
                }

                item {
                    Button(
                        onClick = {
                            // --- DEBUG LOGS ---
                            Log.d("CREATE_EVENT", "Attempting to create event:")
                            Log.d("CREATE_EVENT", "Raw startDate: $startDate")
                            Log.d("CREATE_EVENT", "Raw startTime: $startTime")
                            Log.d("CREATE_EVENT", "Raw endDate: $endDate")
                            Log.d("CREATE_EVENT", "Raw endTime: $endTime")
                            // --- END DEBUG LOGS ---

                            // Use java.text.SimpleDateFormat
                            // This formatter expects strings like "yyyy-MM-dd HH:mm"
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) // Use Locale.US for consistency

                            var startTimestamp: Timestamp? = null
                            if (startDate.isNotBlank() && startTime.isNotBlank()) {
                                val combinedStartString = "$startDate $startTime"
                                Log.d("CREATE_EVENT", "Combined start string to parse: $combinedStartString")
                                startTimestamp = runCatching {
                                    val parsedDate = formatter.parse(combinedStartString)
                                    if (parsedDate != null) Timestamp(parsedDate) else null
                                }.onFailure { Log.e("CREATE_EVENT", "Failed to parse start timestamp", it) }.getOrNull()
                            }

                            var endTimestamp: Timestamp? = null
                            if (endDate.isNotBlank() && endTime.isNotBlank()) {
                                val combinedEndString = "$endDate $endTime"
                                Log.d("CREATE_EVENT", "Combined end string to parse: $combinedEndString")
                                endTimestamp = runCatching {
                                    val parsedDate = formatter.parse(combinedEndString)
                                    if (parsedDate != null) Timestamp(parsedDate) else null
                                }.onFailure { Log.e("CREATE_EVENT", "Failed to parse end timestamp", it) }.getOrNull()
                            }

                            Log.d("CREATE_EVENT", "Parsed startTimestamp: $startTimestamp")
                            Log.d("CREATE_EVENT", "Parsed endTimestamp: $endTimestamp")


                            val geoPoint = if (latitude.isNotBlank() && longitude.isNotBlank()) {
                                runCatching { GeoPoint(latitude.toDouble(), longitude.toDouble()) }.getOrNull()
                            } else null

                            // Fetch current user's display name for ownerName
                            val eventOwnerName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"

                            val event = Event(
                                name = name.trim(),
                                description = description.trim(),
                                category = category,
                                ownerId = currentUserId,
                                startTime = startTimestamp,
                                endTime = endTimestamp,
                                address = address.trim(),
                                location = geoPoint,
                                isPublic = isPublic,
                                ownerName = eventOwnerName, // Use fetched or default name
                                createdAt = Timestamp.now() // Good practice to set createdAt
                            )
                            viewModel.createEvent(event)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isLoading) "Creating..." else "Create Event")
                    }
                }

                if (errorMessage != null) {
                    item {
                        Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                    }
                }
                item { Spacer(modifier = Modifier.height(50.dp)) } // Add some space at the bottom for scrolling
            }
        }
    }
}