package com.example.rievent.ui.utils

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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DatePickerField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    onTextChange: (String) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()


    if (value.isNotBlank()) {
        try {

            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            calendar.time = parser.parse(value) ?: Calendar.getInstance().time
        } catch (e: Exception) {

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

            val outputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formattedDate = outputFormatter.format(selectedCalendar.time)
            onDateSelected(formattedDate)
            onTextChange(formattedDate)
        },
        year,
        month,
        day
    )

    OutlinedTextField(
        value = value,
        onValueChange = { onTextChange(it) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select Date",
                modifier = Modifier.clickable { datePickerDialog.show() }
            )
        }
    )
}