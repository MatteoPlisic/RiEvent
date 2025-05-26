package com.example.rievent.ui.utils // Or your actual utils package

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Locale

@Composable
fun TimePickerField(
    label: String,
    value: String, // This will display the "HH:mm" formatted time
    onTimeSelected: (String) -> Unit, // Callback with "HH:mm"
    onTextChange: (String) -> Unit // If you still need manual text input
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // If value is not empty and is a valid time, try to parse it
    if (value.isNotBlank() && value.matches(Regex("\\d{2}:\\d{2}"))) {
        try {
            val parts = value.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
        } catch (e: Exception) {
            // If parsing fails, use current time
        }
    }

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHourOfDay: Int, selectedMinute: Int ->
            // Format the selected time to "HH:mm" (24-hour format)
            val formattedTime = String.format(Locale.US, "%02d:%02d", selectedHourOfDay, selectedMinute)
            onTimeSelected(formattedTime)
            onTextChange(formattedTime) // Also update the text field if needed
        },
        hour,
        minute,
        true // true for 24-hour format
    )

    OutlinedTextField(
        value = value,
        onValueChange = { onTextChange(it) }, // Allows manual editing if desired
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true, // Make it read-only to force use of picker
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Select Time",
                modifier = Modifier.clickable { timePickerDialog.show() }
            )
        }
    )
}