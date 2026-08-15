package com.wheelsspin.namepicker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wheelsspin.namepicker.PickerViewModel
import kotlinx.coroutines.launch

@Composable
fun WheelScreen(vm: PickerViewModel, modifier: Modifier = Modifier) {
    // Composition-scoped, so it carries the MonotonicFrameClock that the spin animation needs.
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        Wheel(
            entries = vm.names,
            rotationDegrees = vm.rotation.value,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { scope.launch { vm.spin() } },
            enabled = !vm.isSpinning && vm.names.size >= 2,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(),
        ) {
            Text(
                text = if (vm.isSpinning) "Spinning…" else "SPIN",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (vm.names.size < 2) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Add at least two names to spin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        if (vm.history.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recent picks", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { vm.clearHistory() }) { Text("Clear") }
            }
            vm.history.take(10).forEachIndexed { index, name ->
                Text(
                    text = "${index + 1}.  $name",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    vm.winner?.let { winner ->
        AlertDialog(
            onDismissRequest = { vm.dismissWinner() },
            title = {
                Text(
                    text = "Winner",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
            text = {
                Text(
                    text = winner.name,
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.dismissWinner() }) { Text("OK") }
            },
        )
    }
}
