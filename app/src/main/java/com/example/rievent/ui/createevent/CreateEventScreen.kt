package com.example.rievent.ui.createevent

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onCreated: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    val factory = remember { CreateEventViewModelFactory(context.applicationContext as Application) }
    val viewModel: CreateEventViewModel = viewModel(factory = factory)

    // Collect the single state object.
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? -> viewModel.onImageSelected(uri) }
    )

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
    val focusManager = LocalFocusManager.current

    // This effect listens for the success signal to navigate away.
    LaunchedEffect(uiState.creationSuccess) {
        if (uiState.creationSuccess) {
            onCreated() // Call the navigation callback
            viewModel.eventCreationNavigated() // Tell the ViewModel to reset its state
        }
    }

    Drawer(title = "Create Event", navController = navController, gesturesEnabled = true) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 70.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                item { OutlinedTextField(value = uiState.name, onValueChange = viewModel::onNameChange, label = { Text("Event Name") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = uiState.description, onValueChange = viewModel::onDescriptionChange, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                item {
                    ExposedDropdownMenuBox(expanded = uiState.isCategoryMenuExpanded, onExpandedChange = { viewModel.onCategoryMenuToggled(it) }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.category, onValueChange = {}, readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(uiState.isCategoryMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = uiState.isCategoryMenuExpanded, onDismissRequest = { viewModel.onCategoryMenuToggled(false) }) {
                            categoryOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { viewModel.onCategoryChange(option) })
                            }
                        }
                    }
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.addressInput,
                            onValueChange = { viewModel.onAddressInputChange(it) },
                            label = { Text("Address") },
                            modifier = Modifier.fillMaxWidth().onFocusChanged { viewModel.onAddressFocusChanged(it.isFocused) },
                            trailingIcon = {
                                if (uiState.addressInput.isNotEmpty()) {
                                    IconButton(onClick = {
                                        viewModel.onClearAddress()
                                        focusManager.clearFocus()
                                    }) { Icon(Icons.Filled.Clear, "Clear address") }
                                }
                            },
                            singleLine = true
                        )
                        if (uiState.isFetchingPredictions) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 2.dp))
                        }
                        if (uiState.showPredictionsList && uiState.addressPredictions.isNotEmpty() && !uiState.isFetchingPredictions) {
                            Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(max = 200.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                                LazyColumn {
                                    items(items = uiState.addressPredictions, key = { it.placeId }) { prediction ->
                                        Text(
                                            text = prediction.getFullText(null).toString(),
                                            modifier = Modifier.fillMaxWidth().clickable {
                                                viewModel.onPredictionSelected(prediction)
                                                focusManager.clearFocus()
                                            }.padding(horizontal = 16.dp, vertical = 12.dp)
                                        )
                                        if (uiState.addressPredictions.last() != prediction) Divider()
                                    }
                                }
                            }
                        }
                        if (uiState.selectedPlace != null) {
                            Text(
                                "Location: Lat: ${uiState.selectedPlace!!.latLng?.latitude}, Lng: ${uiState.selectedPlace!!.latLng?.longitude}",
                                style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(0.8f).aspectRatio(16f / 9f).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.imageUri != null) {
                            Image(painter = rememberAsyncImagePainter(model = uiState.imageUri), contentDescription = "Selected event image", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = "Add Image Placeholder", modifier = Modifier.size(48.dp))
                                Text("Tap to select image")
                            }
                        }
                    }
                }
                item { DatePickerField(label = "Start Date", value = uiState.startDate, onDateSelected = viewModel::onStartDateChange, onTextChange = viewModel::onStartDateChange) }
                item { TimePickerField(label = "Start Time", value = uiState.startTime, onTimeSelected = viewModel::onStartTimeChange, onTextChange = viewModel::onStartTimeChange) }
                item { DatePickerField(label = "End Date", value = uiState.endDate, onDateSelected = viewModel::onEndDateChange, onTextChange = viewModel::onEndDateChange) }
                item { TimePickerField(label = "End Time", value = uiState.endTime, onTimeSelected = viewModel::onEndTimeChange, onTextChange = viewModel::onEndTimeChange) }

                item {
                    Button(
                        onClick = { viewModel.createEvent() },
                        enabled = !uiState.isSubmitting && uiState.isFormValid,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text(if (uiState.isSubmitting) "Creating..." else "Create Event") }
                }
                if (uiState.userMessage != null) {
                    item { Text("Error: ${uiState.userMessage}", color = MaterialTheme.colorScheme.error) }
                }
                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        }
    }
}