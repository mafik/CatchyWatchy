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

package eu.mrogalski.watchy

import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import eu.mrogalski.watchy.cdm.getAssociatedDevices

class BootReceiver : BroadcastReceiver() {

  @RequiresApi(Build.VERSION_CODES.O)
  override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
      Intent.ACTION_MY_PACKAGE_REPLACED, Intent.ACTION_BOOT_COMPLETED -> {
        android.util.Log.d("CatchyWatchy", "BootReceiver triggered by: ${intent.action}")
        initializeDevicePresenceMonitoring(context)
      }
      else -> {
        android.util.Log.d(
                "CatchyWatchy",
                "BootReceiver received unhandled action: ${intent.action}"
        )
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun initializeDevicePresenceMonitoring(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val deviceManager = context.getSystemService<CompanionDeviceManager>()
      deviceManager?.let { manager ->
        try {
          val associatedDevices = manager.getAssociatedDevices()
          associatedDevices.forEach { deviceInfo ->
            val address = deviceInfo.address

            if (address.isNotBlank()) {
              try {
                manager.startObservingDevicePresence(address)
                android.util.Log.d(
                        "CatchyWatchy",
                        "BroadcastReceiver: Started monitoring device presence for: $address"
                )
              } catch (e: Exception) {
                android.util.Log.e(
                        "CatchyWatchy",
                        "BroadcastReceiver: Failed to start monitoring for device $address",
                        e
                )
              }
            }
          }
          android.util.Log.d(
                  "CatchyWatchy",
                  "BroadcastReceiver: Device presence monitoring initialized for ${associatedDevices.size} devices"
          )
        } catch (e: Exception) {
          android.util.Log.e(
                  "CatchyWatchy",
                  "BroadcastReceiver: Failed to initialize device presence monitoring",
                  e
          )
        }
      }
    } else {
      android.util.Log.w(
              "CatchyWatchy",
              "BroadcastReceiver: Device presence monitoring requires Android 12 (API 31) or higher"
      )
    }
  }
}
