package com.example.rievent.ui.utils // Or your actual utils package

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat // Use java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DatePickerField(
    label: String,
    value: String, // This will display the "yyyy-MM-dd" formatted date
    onDateSelected: (String) -> Unit, // Callback with "yyyy-MM-dd"
    onTextChange: (String) -> Unit // If you still need manual text input
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // If value is not empty and is a valid date, try to parse it to set initial dialog date
    if (value.isNotBlank()) {
        try {
            // Use a formatter that can parse the "yyyy-MM-dd" format
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            calendar.time = parser.parse(value) ?: Calendar.getInstance().time
        } catch (e: Exception) {
            // If parsing fails, use current date
            // Log.e("DatePickerField", "Failed to parse initial date value: $value", e)
        }
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            // Format the selected date to "yyyy-MM-dd"
            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US) // Use a specific Locale for consistency
            val formattedDate = outputFormatter.format(selectedCalendar.time)
            onDateSelected(formattedDate)
            onTextChange(formattedDate) // Also update the text field if needed
        },
        year,
        month,
        day
    )

    OutlinedTextField(
        value = value,
        onValueChange = { onTextChange(it) }, // Allows manual editing if desired
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true, // Make it read-only to force use of picker
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date",
                modifier = Modifier.clickable { datePickerDialog.show() }
            )
        }
    )
}