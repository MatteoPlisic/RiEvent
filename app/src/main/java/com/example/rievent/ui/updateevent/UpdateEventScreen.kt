package com.example.rievent.ui.updateevent

import Event
import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.ui.utils.DatePickerField
import com.example.rievent.ui.utils.TimePickerField
import com.google.firebase.firestore.GeoPoint
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateEventScreen(
    eventId: String,
    viewModel: UpdateEventViewModel = viewModel(),
    onUpdated: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val event by viewModel.event.collectAsState()

    // Load event once
    LaunchedEffect(Unit) {
        viewModel.loadEvent(eventId)
    }

    // Navigate away on success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.resetState()
            onUpdated()
        }
    }

    // Form state
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

    // Fill form once event is loaded
    LaunchedEffect(event) {
        event?.let {
            name = it.name
            description = it.description
            category = it.category
            address = it.address
            isPublic = it.isPublic
            latitude = it.location?.latitude?.toString() ?: ""
            longitude = it.location?.longitude?.toString() ?: ""

            it.startTime?.toDate()?.let { d ->
                startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d)
                startTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
            }

            it.endTime?.toDate()?.let { d ->
                endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(d)
                endTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(d)
            }
        }
    }

    if (event == null && isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
            TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
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

            DatePickerField("Start Date", startDate, { startDate = it }, { startDate = it })
            TimePickerField("Start Time", startTime, { startTime = it }, { startTime = it })

            DatePickerField("End Date", endDate, { endDate = it }, { endDate = it })
            TimePickerField("End Time", endTime, { endTime = it }, { endTime = it })

            TextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
            TextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude") })
            TextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude") })

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                Text("Public Event")
            }

            Button(
                onClick = {
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val startTimestamp = runCatching {
                        com.google.firebase.Timestamp(formatter.parse("$startDate $startTime")!!)
                    }.getOrNull()
                    val endTimestamp = runCatching {
                        com.google.firebase.Timestamp(formatter.parse("$endDate $endTime")!!)
                    }.getOrNull()
                    val geoPoint = if (latitude.isNotBlank() && longitude.isNotBlank()) {
                        GeoPoint(latitude.toDouble(), longitude.toDouble())
                    } else null

                    val updatedEvent = event!!.copy(
                        name = name,
                        description = description,
                        category = category,
                        startTime = startTimestamp,
                        endTime = endTimestamp,
                        address = address,
                        location = geoPoint,
                        isPublic = isPublic
                    )

                    viewModel.updateEvent(updatedEvent)
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Updating..." else "Update Event")
            }

            errorMessage?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
