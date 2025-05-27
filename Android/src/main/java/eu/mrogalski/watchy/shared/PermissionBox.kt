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

package eu.mrogalski.watchy.shared

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun MultiPermissionBox(permissions: List<String>, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionStates by remember {
        mutableStateOf(
                permissions.associateWith { permission ->
                    ContextCompat.checkSelfPermission(context, permission) ==
                            PackageManager.PERMISSION_GRANTED
                }
        )
    }

    val launcher =
            rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                permissionStates =
                        permissionStates.toMutableMap().apply {
                            results.forEach { (permission, isGranted) ->
                                this[permission] = isGranted
                            }
                        }
            }

    val missingPermissions = permissionStates.filter { !it.value }.keys.toList()

    if (missingPermissions.isEmpty()) {
        content()
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                        text = "This feature requires the following permissions:",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                )
                missingPermissions.forEach { permission ->
                    val permissionName =
                            when (permission) {
                                "android.permission.BLUETOOTH_CONNECT" -> "Bluetooth Connect"
                                "android.permission.BLUETOOTH_SCAN" -> "Bluetooth Scan"
                                "android.permission.ACCESS_FINE_LOCATION" -> "Fine Location"
                                "android.permission.ACCESS_COARSE_LOCATION" -> "Coarse Location"
                                "android.permission.POST_NOTIFICATIONS" -> "Post Notifications"
                                "net.dinglisch.android.tasker.PERMISSION_RUN_TASKS" ->
                                        "Run Tasker Tasks"
                                else -> permission.substringAfterLast(".")
                            }
                    Text(
                            text = "• $permissionName",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Button(
                        onClick = { launcher.launch(missingPermissions.toTypedArray()) },
                        modifier = Modifier.padding(top = 16.dp)
                ) { Text("Grant Permissions") }
            }
        }
    }
}
