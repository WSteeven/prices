package com.example.pricesapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.pricesapp.data.UnitMeasure

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropdown(
    units: List<UnitMeasure>,
    selectedUnitId: Long?,
    onUnitSelected: (Long?) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = units.find { it.id == selectedUnitId }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} (${it.abbreviation})" } ?: "Sin unidad",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Unidad de medida") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Sin unidad") },
                onClick = {
                    onUnitSelected(null)
                    expanded = false
                }
            )
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text("${unit.name} (${unit.abbreviation})") },
                    onClick = {
                        onUnitSelected(unit.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
