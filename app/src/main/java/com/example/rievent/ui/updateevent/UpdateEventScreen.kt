package com.example.rievent.ui.updateevent

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.rievent.R
import com.example.rievent.ui.createevent.ImagePreview
import com.example.rievent.ui.utils.DatePickerField
import com.example.rievent.ui.utils.Drawer
import com.example.rievent.ui.utils.TimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateEventScreen(
    eventId: String,
    onUpdated: () -> Unit,
    navController: NavController,
    viewModel: UpdateEventViewModel = viewModel(factory = UpdateEventViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris: List<Uri> ->
            uris.forEach { viewModel.onImageAdded(it) }
        }
    )

    val categoryOptions = listOf("Sports", "Academic", "Business", "Culture", "Concert", "Quizz", "Party")
    val focusManager = LocalFocusManager.current

    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
    }

    LaunchedEffect(uiState.updateSuccess) {
        if (uiState.updateSuccess) {
            onUpdated()
            viewModel.onUpdateNavigated()
        }
    }

    Drawer(title = stringResource(id = R.string.update_event_title), navController = navController, gesturesEnabled = true) { padding ->
        when {
            uiState.isInitialLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            uiState.originalEvent == null -> {
                Box(Modifier.fillMaxSize().padding(padding).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(uiState.userMessage ?: stringResource(id = R.string.event_load_error), color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(top = 70.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        item { OutlinedTextField(value = uiState.name, onValueChange = viewModel::onNameChange, label = { Text(stringResource(id = R.string.event_name_label)) }, modifier = Modifier.fillMaxWidth()) }
                        item { OutlinedTextField(value = uiState.description, onValueChange = viewModel::onDescriptionChange, label = { Text(stringResource(id = R.string.description_label)) }, modifier = Modifier.fillMaxWidth(), minLines = 3) }
                        item {
                            ExposedDropdownMenuBox(expanded = uiState.isCategoryMenuExpanded, onExpandedChange = viewModel::onCategoryMenuToggled, modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.category, onValueChange = {}, readOnly = true, label = { Text(stringResource(id = R.string.category_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(uiState.isCategoryMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(expanded = uiState.isCategoryMenuExpanded, onDismissRequest = { viewModel.onCategoryMenuToggled(false) }) {
                                    categoryOptions.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { viewModel.onCategoryChange(option) }) }
                                }
                            }
                        }
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.addressInput, onValueChange = viewModel::onAddressInputChange,
                                    label = { Text(stringResource(id = R.string.address_label)) },
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { viewModel.onAddressFocusChanged(it.isFocused) },
                                    trailingIcon = { if (uiState.addressInput.isNotEmpty()) { IconButton(onClick = { viewModel.onClearAddress(); focusManager.clearFocus() }) { Icon(Icons.Filled.Clear, stringResource(id = R.string.clear_address_description)) } } },
                                    singleLine = true
                                )
                                if (uiState.isFetchingPredictions) { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp)) }
                                if (uiState.showPredictionsList && uiState.addressPredictions.isNotEmpty()) {
                                    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(max = 200.dp), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                                        LazyColumn {
                                            items(items = uiState.addressPredictions, key = { it.placeId }) { prediction ->
                                                Text(
                                                    text = prediction.getFullText(null).toString(),
                                                    modifier = Modifier.fillMaxWidth().clickable { viewModel.onPredictionSelected(prediction); focusManager.clearFocus() }.padding(16.dp)
                                                )
                                                if (uiState.addressPredictions.last() != prediction) Divider()
                                            }
                                        }
                                    }
                                }
                                if (uiState.selectedPlace != null) {
                                    Text(stringResource(id = R.string.new_location_selected, uiState.selectedPlace!!.latLng?.latitude.toString(), uiState.selectedPlace!!.latLng?.longitude.toString()), style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
                                }
                            }
                        }
                        item {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(id = R.string.event_image_label), style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(8.dp))

                                if (uiState.existingImageUrls.isEmpty() && uiState.newImageUris.isEmpty()) {
                                    Text("No images for this event.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(uiState.existingImageUrls) { url ->
                                            ImagePreview(uri = Uri.parse(url), onRemoveClick = { viewModel.onExistingImageRemoved(url) })
                                        }
                                        items(uiState.newImageUris) { uri ->
                                            ImagePreview(uri = uri, onRemoveClick = { viewModel.onNewImageRemoved(uri) })
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(id = R.string.add_image_placeholder_description))
                                }
                            }
                        }
                        item { DatePickerField(label = stringResource(id = R.string.start_date_label), value = uiState.startDate, onDateSelected = viewModel::onStartDateChange, onTextChange = viewModel::onStartDateChange) }
                        item { TimePickerField(label = stringResource(id = R.string.start_time_label), value = uiState.startTime, onTimeSelected = viewModel::onStartTimeChange, onTextChange = viewModel::onStartTimeChange) }
                        item { DatePickerField(label = stringResource(id = R.string.end_date_label), value = uiState.endDate, onDateSelected = viewModel::onEndDateChange, onTextChange = viewModel::onEndDateChange) }
                        item { TimePickerField(label = stringResource(id = R.string.end_time_label), value = uiState.endTime, onTimeSelected = viewModel::onEndTimeChange, onTextChange = viewModel::onEndTimeChange) }

                        item {
                            Button(
                                onClick = viewModel::updateEvent,
                                enabled = !uiState.isUpdating && uiState.isFormValid,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text(if (uiState.isUpdating) stringResource(id = R.string.updating_button_text) else stringResource(id = R.string.update_event_button)) }
                        }
                        uiState.userMessage?.let {
                            if (!uiState.isInitialLoading) {
                                item { Text(stringResource(id = R.string.generic_error_prefix, it), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                        item { Spacer(Modifier.height(60.dp)) }
                    }
                }
            }
        }
    }
}