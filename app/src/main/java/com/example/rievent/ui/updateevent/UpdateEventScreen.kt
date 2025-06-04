package com.example.rievent.ui.updateevent

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rievent.ui.utils.DatePickerField
import com.example.rievent.ui.utils.TimePickerField
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateEventScreen(
    eventId: String,
    viewModel: UpdateEventViewModel = viewModel(),
    onUpdated: () -> Unit,
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val eventFromVm by viewModel.event.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.resetState()
            onUpdated()
        }
    }

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
    var categoryExpanded by remember { mutableStateOf(false) }

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")

    LaunchedEffect(eventFromVm) {
        eventFromVm?.let { loadedEvent ->
            name = loadedEvent.name
            description = loadedEvent.description
            category = loadedEvent.category
            address = loadedEvent.address
            isPublic = loadedEvent.isPublic
            latitude = loadedEvent.location?.latitude?.toString() ?: ""
            longitude = loadedEvent.location?.longitude?.toString() ?: ""

            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

            loadedEvent.startTime?.toDate()?.let { d ->
                startDate = dateFormatter.format(d)
                startTime = timeFormatter.format(d)
            } ?: run {
                startDate = ""
                startTime = ""
            }

            loadedEvent.endTime?.toDate()?.let { d ->
                endDate = dateFormatter.format(d)
                endTime = timeFormatter.format(d)
            } ?: run {
                endDate = ""
                endTime = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Update Event") })
        }
    ) { paddingValues ->
        if (eventFromVm == null && isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (eventFromVm == null && !isLoading && errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Error loading event: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
        } else if (eventFromVm != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Event Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
                item {
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        category = option
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                item { DatePickerField(label = "Start Date", value = startDate, onDateSelected = { startDate = it }, onTextChange = { startDate = it }) }
                item { TimePickerField(label = "Start Time", value = startTime, onTimeSelected = { startTime = it }, onTextChange = { startTime = it }) }

                item { DatePickerField(label = "End Date", value = endDate, onDateSelected = { endDate = it }, onTextChange = { endDate = it }) }
                item { TimePickerField(label = "End Time", value = endTime, onTimeSelected = { endTime = it }, onTextChange = { endTime = it }) }

                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = latitude,
                        onValueChange = { latitude = it },
                        label = { Text("Latitude (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = longitude,
                        onValueChange = { longitude = it },
                        label = { Text("Longitude (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Public Event")
                    }
                }

                item {
                    Button(
                        onClick = {
                            val currentEvent = eventFromVm ?: return@Button

                            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

                            val startTimestamp: Timestamp? = if (startDate.isNotBlank() && startTime.isNotBlank()) {
                                runCatching { Timestamp(formatter.parse("$startDate $startTime")!!) }.getOrNull()
                            } else null

                            val endTimestamp: Timestamp? = if (endDate.isNotBlank() && endTime.isNotBlank()) {
                                runCatching { Timestamp(formatter.parse("$endDate $endTime")!!) }.getOrNull()
                            } else null

                            val geoPoint: GeoPoint? = if (latitude.isNotBlank() && longitude.isNotBlank()) {
                                runCatching { GeoPoint(latitude.toDouble(), longitude.toDouble()) }.getOrNull()
                            } else null

                            val updatedEventData = currentEvent.copy(
                                name = name.trim(),
                                description = description.trim(),
                                category = category,
                                startTime = startTimestamp,
                                endTime = endTimestamp,
                                address = address.trim(),
                                location = geoPoint,
                                isPublic = isPublic
                            )
                            viewModel.updateEvent(updatedEventData)
                        },
                        enabled = !isLoading && eventFromVm != null,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(if (isLoading) "Updating..." else "Update Event")
                    }
                }

                errorMessage?.let {
                    item {
                        Text("Error: $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}