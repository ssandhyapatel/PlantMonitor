@file:SuppressLint("MissingPermission")
package com.example.plantmonitor.ui

import android.annotation.SuppressLint
import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.plantmonitor.data.SensorDataStore
import com.example.plantmonitor.data.SensorReading
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BLEManager(
    private val context: Context,
    private val onUpdate: (Float, Float, Float) -> Unit
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())

    private val serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val characteristicUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")
    private val cccUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

            // Match your advertised name (PlantSensor_01) – keep your original condition
            if (device.name?.contains("PlantSensor", ignoreCase = true) == true) {
                Log.d("BLEManager", "Found device ${device.name} / ${device.address} – connecting")
                stopScan()
                connectToDevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("BLEManager", "Scan failed: $errorCode")
        }
    }

    fun startScan() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
        if (scanner == null) {
            Log.e("BLEManager", "BluetoothLeScanner is null")
            return
        }

        val scanFilter = ScanFilter.Builder().build() // broad filter (your original behavior)
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Log.d("BLEManager", "Scan started")
        handler.postDelayed({ stopScan() }, 10_000)
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
        try {
            scanner?.stopScan(scanCallback)
            Log.d("BLEManager", "Scan stopped")
        } catch (e: Exception) {
            Log.w("BLEManager", "stopScan error: ${e.message}")
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
        Log.d("BLEManager", "Connecting GATT…")
        // AutoConnect=false for faster first connection
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (gatt == null) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLEManager", "GATT error: $status")
                try { gatt.close() } catch (_: Exception) {}
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
                    Log.d("BLEManager", "Connected, requesting MTU & discovering services")
                    // Request a larger MTU before notifications (best effort)
                    gatt.requestMtu(185)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d("BLEManager", "Disconnected")
                    try { gatt.close() } catch (_: Exception) {}
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d("BLEManager", "MTU changed: $mtu (status=$status)")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (gatt == null) return
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLEManager", "Service discovery failed: $status")
                return
            }

            val service = gatt.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(characteristicUUID)
            if (service == null || characteristic == null) {
                Log.e("BLEManager", "Required service/characteristic not found")
                return
            }

            // Enable notifications: setCharacteristicNotification + write CCC (0x2902)
            val notifOk = gatt.setCharacteristicNotification(characteristic, true)
            Log.d("BLEManager", "setCharacteristicNotification=$notifOk")

            val ccc = characteristic.getDescriptor(cccUUID)
            if (ccc != null) {
                ccc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val wrote = gatt.writeDescriptor(ccc)
                Log.d("BLEManager", "writeDescriptor(CCC)=$wrote")
            } else {
                Log.w("BLEManager", "CCC (0x2902) not found; notifications may not arrive on some phones")
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            Log.d("BLEManager", "onDescriptorWrite status=$status for ${descriptor?.uuid}")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            if (characteristic == null || characteristic.uuid != characteristicUUID) return
            val bytes = characteristic.value ?: return

            val message = try {
                bytes.toString(Charset.forName("UTF-8")).trim()
            } catch (_: Exception) {
                String(bytes).trim()
            }

            try {
                val parts = message.split(",")
                if (parts.size < 3) return

                val voc = parts[0].toFloat()
                val temp = parts[1].toFloat()
                val humidity = parts[2].toFloat()

                // Feed your in-memory store (kept exactly as you had it)
                val reading = SensorReading(System.currentTimeMillis(), voc, temp, humidity)
                SensorDataStore.readings.add(reading)
                if (SensorDataStore.readings.size > 100) {
                    SensorDataStore.readings.removeAt(0)
                }

                // Alerts (kept)
                val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                if (voc > 50f) {
                    AlertsActivity.recentAlerts.add(AlertItem("VOC", "VOC Spike – ${voc.toInt()} ppm", "at $now"))
                }
                if (temp > 35f) {
                    AlertsActivity.recentAlerts.add(AlertItem("TEMP", "Overheating – ${String.format("%.1f", temp)}°C", "at $now"))
                }
                if (humidity < 40f) {
                    AlertsActivity.recentAlerts.add(AlertItem("HUMIDITY", "Low Humidity – ${humidity.toInt()}%", "at $now"))
                }

                // UI callback (kept)
                onUpdate(voc, temp, humidity)

            } catch (e: Exception) {
                Log.e("BLEManager", "Invalid BLE data: $message")
            }
        }
    }
}


