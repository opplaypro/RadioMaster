package com.radiomaster.app.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextButtonDefaults
import androidx.compose.material3.materialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.radiomaster.app.R

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    hint: String = "Szukaj stacji, kraju lub gatunku",
    modifier: Modifier = Modifier
) {
    var fieldValue by remember { mutableStateOf(value) }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            onValueChange(it)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(text = hint) },
        leadingIcon = {
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_search),
                contentDescription = "Szukaj"
            )
        },
        trailingIcon = {
            if (fieldValue.isNotEmpty()) {
                TextButton(
                    onClick = {
                        fieldValue = ""
                        onValueChange("")
                    },
                    colors = TextButtonDefaults.textButtonColors()
                ) {
                    androidx.compose.material3.Icon(
                        painter = androidx.compose.ui.unit.painterResource(id = R.drawable.ic_clear),
                        contentDescription = "Wyczyść"
                    )
                }
            }
        },
        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Search
        ),
        onImeAction = { onSearch() }
    )
}
