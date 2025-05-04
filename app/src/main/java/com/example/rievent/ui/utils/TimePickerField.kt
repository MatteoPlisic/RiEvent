package com.example.rievent.ui.utils

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun TimePickerField(
    label: String,
    value: String,
    onTimeSelected: (String) -> Unit,
    onTextChange: (String) -> Unit,
    error: String? = null
) {
    val context = LocalContext.current

    OutlinedTextField(
        value = value,
        onValueChange = onTextChange,
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = {
                val calendar = Calendar.getInstance()
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val formattedTime = String.format("%02d:%02d", hour, minute)
                        onTimeSelected(formattedTime)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }) {
                Icon(Icons.Default.AccessTime, contentDescription = "Select time")
            }
        },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )

    error?.let {
        Text(
            text = it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
