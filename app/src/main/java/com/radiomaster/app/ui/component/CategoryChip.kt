package com.radiomaster.app.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.rememberScrollState
import androidx.compose.material3.Chip
import androidx.compose.material3.ChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.materialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CategoryChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        modifier = modifier.clickable { onClick() },
        colors = if (selected) ChipDefaults.colors(containerColor = materialTheme.colorScheme.primaryContainer) else ChipDefaults.colors(),
        label = { Text(text = name) },
        onClick = { onClick() },
        selected = selected
    )
}

@Composable
fun CategoriesRow(
    categories: List<String>,
    selected: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            categories.forEach { category ->
                CategoryChip(
                    name = category,
                    selected = selected == category,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
