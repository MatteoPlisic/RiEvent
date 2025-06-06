package com.example.rievent.ui.updateevent

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.rievent.ui.utils.DatePickerField
import com.example.rievent.ui.utils.Drawer
import com.example.rievent.ui.utils.TimePickerField
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import java.util.Locale

// Assume Event data class exists and has imageUrl: String?
// data class Event(..., var imageUrl: String? = null, ...)
// Assume AutocompletePrediction and Place are defined elsewhere or part of a library
// For example, from com.google.android.libraries.places.api.model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateEventScreen(
    eventId: String,
    viewModel: UpdateEventViewModel = viewModel(), // You might need a factory here
    onUpdated: () -> Unit,
    navController: NavController // Added for Drawer consistency
) {
    val context = LocalContext.current

    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val eventFromVm by viewModel.event.collectAsState()

    // States for address predictions (to be provided by UpdateEventViewModel)
    val addressPredictions by viewModel.addressPredictions.collectAsState()
    val isFetchingPredictions by viewModel.isFetchingPredictions.collectAsState()

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.resetState()
            onUpdated()
        }
    }

    // UI States
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }

    var addressInput by remember { mutableStateOf("") }
    var userSelectedAddress by remember { mutableStateOf(false) }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var showPredictionsList by remember { mutableStateOf(false) }

    var isPublic by remember { mutableStateOf(true) }
    var categoryExpanded by remember { mutableStateOf(false) }

    var newImageUri by remember { mutableStateOf<Uri?>(null) }
    var currentImageUrlFromVm by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        newImageUri = uri
        if (uri != null) currentImageUrlFromVm = null // If new image is picked, don't use old URL for display logic
    }

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(eventFromVm) {
        eventFromVm?.let { loadedEvent ->
            name = loadedEvent.name
            description = loadedEvent.description
            category = loadedEvent.category
            isPublic = loadedEvent.isPublic
            currentImageUrlFromVm = loadedEvent.imageUrl
            newImageUri = null // Reset any pending new image if event data reloads

            addressInput = loadedEvent.address
            if (loadedEvent.location != null) {
                latitude = loadedEvent.location.latitude.toString()
                longitude = loadedEvent.location.longitude.toString()
                userSelectedAddress = true
            } else {
                latitude = ""
                longitude = ""
                userSelectedAddress = false
            }

            // Use java.text.SimpleDateFormat for broader compatibility if android.icu not guaranteed
            val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val timeFormatter = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())

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

    Drawer(
        title = "Update Event",
        navController = navController,
        gesturesEnabled = true,
        content = {
            if (isLoading && eventFromVm == null) {
                Box(modifier = Modifier.fillMaxSize().padding(top = 70.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (!isLoading && eventFromVm == null && errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 70.dp).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error loading event: $errorMessage", color = MaterialTheme.colorScheme.error)
                }
            } else if (eventFromVm != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 70.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
                        }
                        item {
                            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        }
                        item {
                            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = category, onValueChange = {}, readOnly = true, label = { Text("Category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                    categoryOptions.forEach { option ->
                                        DropdownMenuItem(text = { Text(option) }, onClick = { category = option; categoryExpanded = false })
                                    }
                                }
                            }
                        }

                        item { // Address Autocomplete Item
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = addressInput,
                                    onValueChange = {
                                        addressInput = it
                                        userSelectedAddress = false
                                        if (!userSelectedAddress) { // Clear lat/lng if user types, unless they select prediction
                                            latitude = ""
                                            longitude = ""
                                        }
                                        if (it.isNotBlank()) {
                                            viewModel.fetchAddressPredictions(it)
                                            showPredictionsList = true
                                        } else {
                                            viewModel.clearAddressSearchStates()
                                            showPredictionsList = false
                                        }
                                    },
                                    label = { Text("Address") },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            if (addressInput.isNotBlank() && addressPredictions.isNotEmpty()) showPredictionsList = true
                                        } else {
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(200)
                                                if (showPredictionsList) showPredictionsList = false
                                            }
                                        }
                                    },
                                    trailingIcon = {
                                        if (addressInput.isNotEmpty()) {
                                            IconButton(onClick = {
                                                addressInput = ""; userSelectedAddress = false; latitude = ""; longitude = ""
                                                viewModel.clearAddressSearchStates(); showPredictionsList = false; focusManager.clearFocus()
                                            }) { Icon(Icons.Filled.Clear, "Clear address") }
                                        }
                                    },
                                    singleLine = true
                                )
                                if (isFetchingPredictions) {
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp))
                                }
                                if (showPredictionsList && addressPredictions.isNotEmpty() && !isFetchingPredictions) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(max = 200.dp),
                                        shape = RoundedCornerShape(8.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        LazyColumn {
                                            items(items = addressPredictions, key = { it.placeId }) { prediction ->
                                                Text(
                                                    text = prediction.getFullText(null).toString(),
                                                    modifier = Modifier.fillMaxWidth().clickable {
                                                        viewModel.fetchPlaceDetails(prediction) { place ->
                                                            addressInput = place.address ?: prediction.getPrimaryText(null).toString()
                                                            userSelectedAddress = true
                                                            latitude = place.latLng?.latitude?.toString() ?: ""
                                                            longitude = place.latLng?.longitude?.toString() ?: ""
                                                        }
                                                        showPredictionsList = false; focusManager.clearFocus()
                                                    }.padding(horizontal = 16.dp, vertical = 12.dp)
                                                )
                                                if (addressPredictions.last() != prediction) Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                                if (userSelectedAddress && latitude.isNotBlank() && longitude.isNotBlank()) {
                                    Text("Selected Location: Lat: $latitude, Lng: $longitude", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        item { // Image Picker Item
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Event Image (Optional)", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val imageToDisplay = newImageUri ?: currentImageUrlFromVm?.let { Uri.parse(it) }
                                    if (imageToDisplay != null) {
                                        Image(painter = rememberAsyncImagePainter(model = imageToDisplay), contentDescription = "Event image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add Image Placeholder", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Tap to select image", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                if (newImageUri != null || currentImageUrlFromVm != null) {
                                    Button(onClick = { newImageUri = null; currentImageUrlFromVm = null }, modifier = Modifier.padding(top=4.dp)) {
                                        Text("Remove Image")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        item { DatePickerField(label = "Start Date", value = startDate, onDateSelected = { startDate = it }, onTextChange = { startDate = it }) }
                        item { TimePickerField(label = "Start Time", value = startTime, onTimeSelected = { startTime = it }, onTextChange = { startTime = it }) }
                        item { DatePickerField(label = "End Date", value = endDate, onDateSelected = { endDate = it }, onTextChange = { endDate = it }) }
                        item { TimePickerField(label = "End Time", value = endTime, onTimeSelected = { endTime = it }, onTextChange = { endTime = it }) }

                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp)) {
                                Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Make Event Public")
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    val currentEventDetails = eventFromVm ?: return@Button
                                    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // Use java.text
                                    val startTs = if (startDate.isNotBlank() && startTime.isNotBlank()) runCatching { Timestamp(formatter.parse("$startDate $startTime")!!) }.getOrNull() else null
                                    val endTs = if (endDate.isNotBlank() && endTime.isNotBlank()) runCatching { Timestamp(formatter.parse("$endDate $endTime")!!) }.getOrNull() else null

                                    val finalAddr = addressInput.trim()
                                    val geoPt: GeoPoint? = if (userSelectedAddress && latitude.isNotBlank() && longitude.isNotBlank()) {
                                        runCatching { GeoPoint(latitude.toDouble(), longitude.toDouble()) }.getOrNull()
                                    } else if (!userSelectedAddress && addressInput.isNotBlank() && addressInput == currentEventDetails.address && currentEventDetails.location != null) {
                                        // If address text is same as original and it had a location, keep original location
                                        // This handles case where user focuses out of address field without changing it or selecting a new prediction
                                        currentEventDetails.location
                                    }
                                    else {
                                        null // No valid GeoPoint if not selected from prediction or original retained
                                    }

                                    val updatedEventData = currentEventDetails.copy(
                                        name = name.trim(), description = description.trim(), category = category,
                                        startTime = startTs, endTime = endTs,
                                        address = finalAddr, location = geoPt, isPublic = isPublic
                                        // imageUrl is handled by ViewModel based on newImageUri and removeCurrentImage flag
                                    )
                                    viewModel.updateEventWithOptionalNewImage(
                                        eventData = updatedEventData,
                                        newImageUri = newImageUri,
                                        removeCurrentImage = currentImageUrlFromVm == null && eventFromVm?.imageUrl != null && newImageUri == null
                                    )
                                },
                                enabled = !isLoading && name.isNotBlank() && description.isNotBlank() && category.isNotBlank() && startDate.isNotBlank() && startTime.isNotBlank() && addressInput.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text(if (isLoading) "Updating..." else "Update Event") }
                        }

                        errorMessage?.let {
                            item { Text("Error: $it", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp)) }
                        }
                        item { Spacer(modifier = Modifier.height(60.dp)) }
                    }
                }
            }
        }
    )
}