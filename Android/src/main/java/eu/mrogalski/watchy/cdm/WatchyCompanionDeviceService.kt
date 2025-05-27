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

package eu.mrogalski.watchy.cdm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.companion.AssociationInfo
import android.companion.CompanionDeviceService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat

@RequiresApi(Build.VERSION_CODES.S)
class WatchyCompanionDeviceService : CompanionDeviceService() {

    companion object {
        private const val TAG = "WatchyCompanionDeviceService"
    }

    private val notificationManager: DeviceNotificationManager by lazy {
        DeviceNotificationManager(applicationContext)
    }

    private val bluetoothManager: BluetoothManager by lazy {
        applicationContext.getSystemService()!!
    }

    private val gattCallback =
            object : BluetoothGattCallback() {
                @SuppressLint("MissingPermission")
                override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                ) {
                    val address = gatt?.device?.address ?: "Unknown"
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d(TAG, "GATT Connected to $address")
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.d(TAG, "GATT Disconnected from $address")
                            gatt?.close()
                        }
                        BluetoothProfile.STATE_CONNECTING -> {
                            Log.d(TAG, "GATT Connecting to $address")
                        }
                        BluetoothProfile.STATE_DISCONNECTING -> {
                            Log.d(TAG, "GATT Disconnecting from $address")
                        }
                    }
                }
            }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WatchyCompanionDeviceService created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WatchyCompanionDeviceService destroyed")
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceAppeared(associationInfo: AssociationInfo) {
        super.onDeviceAppeared(associationInfo)
        Log.d(TAG, "Device appeared: ${associationInfo.deviceMacAddress}")

        if (missingPermissions()) {
            return
        }

        val address = associationInfo.deviceMacAddress?.toString() ?: return
        var device: BluetoothDevice? = null
        if (Build.VERSION.SDK_INT >= 34) {
            device = associationInfo.associatedDevice?.bleDevice?.device
        }
        if (device == null) {
            device = bluetoothManager.adapter.getRemoteDevice(address)
        }
        val status = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT)

        if (status == BluetoothProfile.STATE_DISCONNECTED) {
            connectToDevice(address)
        } else if (status == BluetoothProfile.STATE_CONNECTED) {
            // Invoke the "Brama" Tasker task when device is connected
            invokeTaskerTask("Brama")
        }

        notificationManager.onDeviceAppeared(
                address = address,
                status = getStatusDescription(status),
        )
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(address: String) {
        try {
            val bluetoothManager = getSystemService<BluetoothManager>()!!
            val bluetoothAdapter = bluetoothManager.adapter
            // Ensure the address is in uppercase format as required by Android
            val normalizedAddress = address.uppercase()
            val device = bluetoothAdapter.getRemoteDevice(normalizedAddress)

            Log.d(TAG, "Attempting to connect to device: $normalizedAddress")
            device.connectGatt(this, false, gattCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to device $address", e)
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onDeviceDisappeared(associationInfo: AssociationInfo) {
        super.onDeviceDisappeared(associationInfo)
        val address = associationInfo.deviceMacAddress?.toString() ?: "Unknown"
        Log.d(TAG, "Device disappeared: $address")

        if (missingPermissions()) {
            return
        }

        notificationManager.onDeviceDisappeared(
                address = associationInfo.deviceMacAddress?.toString() ?: return,
        )
    }

    /** Convert Bluetooth connection state to human-readable description */
    private fun getStatusDescription(status: Int): String {
        return when (status) {
            BluetoothProfile.STATE_CONNECTED -> "Connected"
            BluetoothProfile.STATE_CONNECTING -> "Connecting"
            BluetoothProfile.STATE_DISCONNECTED -> "Disconnected"
            BluetoothProfile.STATE_DISCONNECTING -> "Disconnecting"
            else -> "Unknown ($status)"
        }
    }

    /**
     * Check BLUETOOTH_CONNECT is granted and POST_NOTIFICATIONS is granted for devices running
     * Android 13 and above.
     */
    private fun missingPermissions(): Boolean {
        val missingBluetoothConnect =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT,
                ) != PackageManager.PERMISSION_GRANTED

        val missingPostNotifications =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED

        if (missingBluetoothConnect || missingPostNotifications) {
            val missingPerms = buildList {
                if (missingBluetoothConnect) add("BLUETOOTH_CONNECT")
                if (missingPostNotifications) add("POST_NOTIFICATIONS")
            }
            Log.w(
                    TAG,
                    "Missing required permissions: ${missingPerms.joinToString(", ")}. " +
                            "Please grant these permissions in the app settings or restart the app to request them."
            )
            return true
        }

        return false
    }

    /**
     * Invoke a pre-defined Tasker task by name Based on:
     * https://tasker.joaoapps.com/invoketasks.html
     */
    private fun invokeTaskerTask(taskName: String) {
        try {
            // Check if Tasker is installed and accessible
            if (!isTaskerAvailable()) {
                Log.w(TAG, "Tasker is not available or external access is blocked")
                return
            }

            // Create intent to invoke Tasker task using the official TaskerIntent format
            val intent =
                    Intent().apply {
                        action = "net.dinglisch.android.tasker.ACTION_TASK"
                        putExtra("task_name", taskName)
                        // Make it an explicit broadcast to Tasker
                        setPackage("net.dinglisch.android.taskerm")
                    }

            // Send broadcast to invoke the task
            sendBroadcast(intent)
            Log.d(TAG, "Invoked Tasker task: $taskName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to invoke Tasker task '$taskName'", e)
        }
    }

    /** Check if Tasker is available and external access is enabled */
    private fun isTaskerAvailable(): Boolean {
        return try {
            // Check if Tasker is installed
            packageManager.getPackageInfo("net.dinglisch.android.taskerm", 0)

            // Check if we have the required permission
            val hasPermission =
                    checkSelfPermission("net.dinglisch.android.tasker.PERMISSION_RUN_TASKS") ==
                            PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                Log.w(TAG, "Missing Tasker permission: PERMISSION_RUN_TASKS")
                return false
            }

            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Tasker is not installed")
            false
        }
    }

    /** Utility class to post notification when CDM notifies that a device appears or disappears */
    private class DeviceNotificationManager(context: Context) {

        companion object {
            private const val CDM_CHANNEL = "cdm_channel"
            private const val TAG = "DeviceNotificationManager"
        }

        private val manager = NotificationManagerCompat.from(context)
        private val context = context

        private val notificationBuilder =
                NotificationCompat.Builder(context, CDM_CHANNEL)
                        .setSmallIcon(
                                IconCompat.createWithResource(context, context.applicationInfo.icon)
                        )
                        .setContentTitle("Catchy Watchy")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        init {
            createNotificationChannel()
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        private fun updateNotification(address: String, status: String) {
            Log.d(TAG, "Updating notification for device $address: $status")
            val notification =
                    notificationBuilder
                            .setContentText("Device: $address\nStatus: $status")
                            .setStyle(
                                    NotificationCompat.BigTextStyle()
                                            .bigText("Device: $address\nStatus: $status")
                            )
            manager.notify(address.hashCode(), notification.build())
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        fun onDeviceAppeared(address: String, status: String) {
            updateNotification(address, "Device appeared - $status")
        }

        @SuppressLint("InlinedApi")
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        fun onDeviceDisappeared(address: String) {
            // Hide the notification when device disappears
            manager.cancel(address.hashCode())
        }

        private fun createNotificationChannel() {
            val channel =
                    NotificationChannelCompat.Builder(
                                    CDM_CHANNEL,
                                    NotificationManager.IMPORTANCE_DEFAULT
                            )
                            .setName("Catchy Watchy Device Presence")
                            .setDescription(
                                    "Notifications when associated devices appear or disappear"
                            )
                            .build()
            manager.createNotificationChannel(channel)
        }
    }
}
