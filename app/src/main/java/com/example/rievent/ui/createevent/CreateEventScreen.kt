package com.example.rievent.ui.createevent

import Event
import android.net.Uri // For Uri
import androidx.activity.compose.rememberLauncherForActivityResult // For image picker
import androidx.activity.result.contract.ActivityResultContracts // For image picker
import androidx.compose.foundation.Image // For image preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // To make image preview clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons // For icons
import androidx.compose.material.icons.filled.AddPhotoAlternate // Example icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // If needed for Coil
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter // For image loading
import com.example.rievent.ui.utils.DatePickerField
import com.example.rievent.ui.utils.Drawer
import com.example.rievent.ui.utils.TimePickerField
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    viewModel: CreateEventViewModel,
    onCreated: () -> Unit,
    currentUserId: String,
    // ... other navigation params
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
    // ... other state variables ...
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

    // State for selected image URI
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // ActivityResultLauncher for picking an image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri // Update the state with the selected image URI
    }

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
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
            imageUri = null // Clear selected image
            viewModel.resetState()
            onCreated()
        }
    }

    Drawer(
        title = "Create Event",
        // ... navigation callbacks ...
        onLogout = onLogout,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToEvents = onNavigateToEvents,
        onNavigateToCreateEvent = onNavigateToCreateEvent,
        onNavigateToMyEvents = onNavigateToMyEvents,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 80.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ... (TextFields for name, description, category dropdown) ...
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


                // Image Picker and Preview
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Event Image (Optional)", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // Adjust width as needed
                                .aspectRatio(16f / 9f) // Common aspect ratio for event images
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { imagePickerLauncher.launch("image/*") }, // Launch image picker
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = imageUri),
                                    contentDescription = "Selected event image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Add Image Placeholder",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text("Tap to select image", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }


                // ... (DatePickerField, TimePickerField, other TextFields) ...
                item {
                    DatePickerField(
                        label = "Start Date",
                        value = startDate,
                        onDateSelected = { selectedDateString -> startDate = selectedDateString },
                        onTextChange = { text -> startDate = text }
                    )
                }

                item {
                    TimePickerField(
                        label = "Start Time",
                        value = startTime,
                        onTimeSelected = { selectedTimeString -> startTime = selectedTimeString },
                        onTextChange = { text -> startTime = text }
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
                            // ... (timestamp parsing logic - unchanged) ...
                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)

                            var startTimestamp: Timestamp? = null
                            if (startDate.isNotBlank() && startTime.isNotBlank()) {
                                val combinedStartString = "$startDate $startTime"
                                startTimestamp = runCatching {
                                    val parsedDate = formatter.parse(combinedStartString)
                                    if (parsedDate != null) Timestamp(parsedDate) else null
                                }.onFailure { Log.e("CREATE_EVENT", "Failed to parse start timestamp", it) }.getOrNull()
                            }

                            var endTimestamp: Timestamp? = null
                            if (endDate.isNotBlank() && endTime.isNotBlank()) {
                                val combinedEndString = "$endDate $endTime"
                                endTimestamp = runCatching {
                                    val parsedDate = formatter.parse(combinedEndString)
                                    if (parsedDate != null) Timestamp(parsedDate) else null
                                }.onFailure { Log.e("CREATE_EVENT", "Failed to parse end timestamp", it) }.getOrNull()
                            }


                            val geoPoint = if (latitude.isNotBlank() && longitude.isNotBlank()) {
                                runCatching { GeoPoint(latitude.toDouble(), longitude.toDouble()) }.getOrNull()
                            } else null

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
                                ownerName = eventOwnerName,
                                createdAt = Timestamp.now(),
                                imageUrl = null // Will be set by ViewModel if imageUri is present
                            )
                            // Call the new ViewModel function
                            viewModel.createEventWithImage(event, imageUri)
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
                item { Spacer(modifier = Modifier.height(50.dp)) }
            }
        }
    }
}