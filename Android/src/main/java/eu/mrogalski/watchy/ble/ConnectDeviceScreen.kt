/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.mrogalski.watchy.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@SuppressLint("MissingPermission")
@Composable
fun ConnectDeviceScreen(device: BluetoothDevice, onBack: () -> Unit) {
    val context = LocalContext.current
    var connectionState by remember { mutableStateOf("Disconnected") }
    var gatt by remember { mutableStateOf<BluetoothGatt?>(null) }
    var services by remember { mutableStateOf<List<String>>(emptyList()) }

    val gattCallback = remember {
        object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectionState = "Connected"
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connectionState = "Disconnected"
                        services = emptyList()
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        connectionState = "Connecting"
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    services = gatt?.services?.map { it.uuid.toString() } ?: emptyList()
                }
            }
        }
    }

    DisposableEffect(device) {
        onDispose {
            gatt?.disconnect()
            gatt?.close()
        }
    }

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "BLE Device Connection", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onBack) { Text("Back") }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "Device Information", style = MaterialTheme.typography.titleMedium)
                Text("Name: ${device.name ?: "Unknown"}")
                Text("Address: ${device.address}")
                Text("Status: $connectionState")
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(
                    onClick = {
                        if (connectionState == "Disconnected") {
                            gatt = device.connectGatt(context, false, gattCallback)
                        }
                    },
                    enabled = connectionState == "Disconnected"
            ) { Text("Connect") }

            Button(onClick = { gatt?.disconnect() }, enabled = connectionState == "Connected") {
                Text("Disconnect")
            }
        }

        if (services.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "Discovered Services", style = MaterialTheme.typography.titleMedium)
                    services.forEach { service ->
                        Text(text = service, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
