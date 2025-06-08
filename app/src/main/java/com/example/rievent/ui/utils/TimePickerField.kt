package com.example.rievent.ui.utils

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
    value: String,
    onTimeSelected: (String) -> Unit,
    onTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()


    if (value.isNotBlank() && value.matches(Regex("\\d{2}:\\d{2}"))) {
        try {
            val parts = value.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
        } catch (e: Exception) {

        }
    }

    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)

    val timePickerDialog = TimePickerDialog(
        context,
        { _, selectedHourOfDay: Int, selectedMinute: Int ->

            val formattedTime = String.format(Locale.US, "%02d:%02d", selectedHourOfDay, selectedMinute)
            onTimeSelected(formattedTime)
            onTextChange(formattedTime)
        },
        hour,
        minute,
        true
    )

    OutlinedTextField(
        value = value,
        onValueChange = { onTextChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Select Time",
                modifier = Modifier.clickable { timePickerDialog.show() }
            )
        }
    )
}