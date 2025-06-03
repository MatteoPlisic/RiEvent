package com.example.rievent.ui.createevent

import Event
import android.app.Application
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onCreated: () -> Unit,
    currentUserId: String,
    onLogout: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val factory = remember { CreateEventViewModelFactory(context.applicationContext as Application) }
    val viewModel: CreateEventViewModel = viewModel(factory = factory)

    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val addressPredictions by viewModel.addressPredictions.collectAsState()
    val isFetchingPredictions by viewModel.isFetchingPredictions.collectAsState()

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
    var expandedCategory by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            name = ""
            description = ""
            category = ""
            startDate = ""
            startTime = ""
            endDate = ""
            endTime = ""
            addressInput = ""
            userSelectedAddress = false
            latitude = ""
            longitude = ""
            isPublic = true
            imageUri = null
            showPredictionsList = false
            viewModel.resetState()
            onCreated()
        }
    }

    Drawer(
        title = "Create Event",
        navController = navController,
        {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 70.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    }
                    item {
                        ExposedDropdownMenuBox(expanded = expandedCategory, onExpandedChange = { expandedCategory = !expandedCategory }, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedCategory) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                                categoryOptions.forEach { option ->
                                    DropdownMenuItem(text = { Text(option) }, onClick = { category = option; expandedCategory = false })
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = addressInput,
                                onValueChange = {
                                    addressInput = it
                                    userSelectedAddress = false
                                    latitude = ""
                                    longitude = ""
                                    if (it.isNotBlank()) {
                                        viewModel.fetchAddressPredictions(it)
                                        showPredictionsList = true
                                    } else {
                                        viewModel.clearAddressSearchStates()
                                        showPredictionsList = false
                                    }
                                },
                                label = { Text("Address") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            if (addressInput.isNotBlank() && addressPredictions.isNotEmpty()) {
                                                showPredictionsList = true
                                            }
                                        } else {
                                            // Hide predictions if focus is lost and it wasn't due to selecting an item.
                                            // A small delay can help ensure click on item processes before list hides.
                                            coroutineScope.launch {
                                                kotlinx.coroutines.delay(200) // Adjust delay as needed
                                                // Only hide if showPredictionsList is still true,
                                                // meaning a prediction wasn't just selected.
                                                if (showPredictionsList) {
                                                    showPredictionsList = false
                                                }
                                            }
                                        }
                                    },
                                trailingIcon = {
                                    if (addressInput.isNotEmpty()) {
                                        IconButton(onClick = {
                                            addressInput = ""
                                            userSelectedAddress = false
                                            latitude = ""
                                            longitude = ""
                                            viewModel.clearAddressSearchStates()
                                            showPredictionsList = false
                                            focusManager.clearFocus()
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
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    LazyColumn {
                                        items(items = addressPredictions, key = { prediction -> prediction.placeId }) { prediction ->
                                            Text(
                                                text = prediction.getFullText(null).toString(),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        viewModel.fetchPlaceDetails(prediction) { place ->
                                                            addressInput = place.address ?: prediction.getPrimaryText(null).toString()
                                                            userSelectedAddress = true
                                                            latitude = place.latLng?.latitude?.toString() ?: ""
                                                            longitude = place.latLng?.longitude?.toString() ?: ""
                                                        }
                                                        showPredictionsList = false
                                                        focusManager.clearFocus()
                                                    }
                                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                            )
                                            if (addressPredictions.last() != prediction) {
                                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }

                            if (userSelectedAddress && latitude.isNotBlank() && longitude.isNotBlank()) {
                                Text(
                                    "Location: Lat: $latitude, Lng: $longitude",
                                    style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Event Image (Optional)", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.8f).aspectRatio(16f / 9f)
                                    .clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUri != null) {
                                    Image(painter = rememberAsyncImagePainter(model = imageUri), contentDescription = "Selected event image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add Image Placeholder", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("Tap to select image", style = MaterialTheme.typography.bodySmall)
                                    }
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
                                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                val startTs = if (startDate.isNotBlank() && startTime.isNotBlank()) runCatching { Timestamp(formatter.parse("$startDate $startTime")!!) }.getOrNull() else null
                                val endTs = if (endDate.isNotBlank() && endTime.isNotBlank()) runCatching { Timestamp(formatter.parse("$endDate $endTime")!!) }.getOrNull() else null
                                val finalAddr = if (userSelectedAddress) addressInput else addressInput.trim()
                                val geoPt = if (latitude.isNotBlank() && longitude.isNotBlank() && userSelectedAddress) runCatching { GeoPoint(latitude.toDouble(), longitude.toDouble()) }.getOrNull() else null
                                val ownerName = FirebaseAuth.getInstance().currentUser?.displayName ?: "Anonymous"

                                val event = Event(
                                    name = name.trim(), description = description.trim(), category = category,
                                    ownerId = currentUserId, startTime = startTs, endTime = endTs,
                                    address = finalAddr, location = geoPt, isPublic = isPublic,
                                    ownerName = ownerName, createdAt = Timestamp.now(), imageUrl = null
                                )
                                viewModel.createEventWithImage(event, imageUri)
                            },
                            enabled = !isLoading && name.isNotBlank() && description.isNotBlank() && category.isNotBlank() && startDate.isNotBlank() && startTime.isNotBlank() && addressInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text(if (isLoading) "Creating..." else "Create Event") }
                    }

                    if (errorMessage != null) {
                        item {
                            Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(60.dp)) }
                }
            }
        },
    )
}