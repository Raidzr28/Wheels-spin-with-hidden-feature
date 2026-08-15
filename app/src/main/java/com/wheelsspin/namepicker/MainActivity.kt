package com.wheelsspin.namepicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wheelsspin.namepicker.ui.NamesScreen
import com.wheelsspin.namepicker.ui.WheelScreen
import com.wheelsspin.namepicker.ui.theme.NamePickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NamePickerTheme {
                NamePickerApp()
            }
        }
    }
}

private enum class Tab { Wheel, Names }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NamePickerApp(vm: PickerViewModel = viewModel()) {
    var tab by rememberSaveable { mutableStateOf(Tab.Wheel) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Name Picker") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove winner after pick") },
                            leadingIcon = {
                                Checkbox(
                                    checked = vm.removeWinnerAfterPick,
                                    onCheckedChange = null,
                                )
                            },
                            onClick = { vm.toggleRemoveWinnerAfterPick() },
                        )
                        DropdownMenuItem(
                            text = { Text("Vibration") },
                            leadingIcon = {
                                Checkbox(checked = vm.hapticsEnabled, onCheckedChange = null)
                            },
                            onClick = { vm.toggleHaptics() },
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.Wheel,
                    onClick = { tab = Tab.Wheel },
                    icon = {
                        Icon(Icons.Filled.RadioButtonChecked, contentDescription = null)
                    },
                    label = { Text("Wheel") },
                )
                NavigationBarItem(
                    selected = tab == Tab.Names,
                    onClick = { tab = Tab.Names },
                    icon = { Icon(Icons.Filled.People, contentDescription = null) },
                    label = { Text("Names") },
                )
            }
        },
    ) { innerPadding ->
        when (tab) {
            Tab.Wheel -> WheelScreen(vm, Modifier.padding(innerPadding))
            Tab.Names -> NamesScreen(vm, Modifier.padding(innerPadding))
        }
    }
}
