package com.wheelsspin.namepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.wheelsspin.namepicker.Entry
import com.wheelsspin.namepicker.PickerViewModel

@Composable
fun NamesScreen(vm: PickerViewModel, modifier: Modifier = Modifier) {
    var draft by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Entry?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add a name") },
            supportingText = { Text("Separate multiple names with commas") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            // The field clears only when the submission was accepted. For a normal name that is
            // always; for a pre-selection command it is the confirmation that it took.
            keyboardActions = KeyboardActions(onDone = {
                if (vm.submitNameField(draft)) draft = ""
            }),
            trailingIcon = {
                IconButton(
                    onClick = { if (vm.submitNameField(draft)) draft = "" },
                    enabled = draft.isNotBlank(),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add name")
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${vm.names.size} ${if (vm.names.size == 1) "name" else "names"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Row {
                TextButton(onClick = { vm.shuffle() }, enabled = vm.names.size > 1) {
                    Text("Shuffle")
                }
                TextButton(onClick = { vm.clearAll() }, enabled = vm.names.isNotEmpty()) {
                    Text("Clear all")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            items(vm.names, key = { it.id }) { entry ->
                NameRow(
                    entry = entry,
                    onEdit = { editing = entry },
                    onDelete = { vm.removeName(entry.id) },
                )
            }
        }
    }

    editing?.let { entry ->
        RenameDialog(
            entry = entry,
            onDismiss = { editing = null },
            onConfirm = { newName ->
                vm.renameName(entry.id, newName)
                editing = null
            },
        )
    }
}

@Composable
private fun NameRow(
    entry: Entry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Rename ${entry.name}")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete ${entry.name}")
            }
        }
    }
}

@Composable
private fun RenameDialog(
    entry: Entry,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(entry.id) { mutableStateOf(entry.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
