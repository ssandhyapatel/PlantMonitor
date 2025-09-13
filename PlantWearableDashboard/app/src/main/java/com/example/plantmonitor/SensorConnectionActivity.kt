
package com.example.plantmonitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.plantmonitor.databinding.ActivitySensorConnectionBinding
// RIGHT

import com.github.mikephil.charting.BuildConfig


import java.util.UUID

class SensorConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySensorConnectionBinding

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothManager.adapter
    }
    private val bleScanner by lazy { bluetoothAdapter?.bluetoothLeScanner }
    private val scanResults = mutableListOf<BluetoothDevice>()
    private val handler = Handler(Looper.getMainLooper())

    private var isScanning = false
    private val scanTimeout: Long = 8000L

    // Service UUID filter for your Arduino/ESP32 (FFE0)
    private val SERVICE_UUID = ParcelUuid(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms.values.all { it }) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            startBleScan()
        } else {
            Toast.makeText(this, "Bluetooth permissions denied", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySensorConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnScan.setOnClickListener {
            checkAndStartScan()
        }

        binding.availableDevice.setOnClickListener {
            val selected = binding.availableDevice.text.toString().split("\n".toRegex()).first()
            simulateConnection(selected) // keep your original behavior
        }

        // Show Skip button in debug mode
        if (BuildConfig.DEBUG) {
            binding.btnSkip.visibility = View.VISIBLE
            binding.btnSkip.setOnClickListener {
                val prefs = getSharedPreferences("plant_prefs", MODE_PRIVATE)
                prefs.edit().putBoolean("isSetupDone", true).apply()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        }

        binding.connectedDevice.text = "PlantSensor_01 (connected)"
    }

    private fun checkAndStartScan() {
        val requiredPermissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isEmpty()) {
            startBleScan()
        } else {
            permissionsLauncher.launch(requiredPermissions)
        }
    }

    private fun startBleScan() {
        if (isScanning) return
        isScanning = true
        scanResults.clear()
        Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show()

        // prefer a service-UUID filter (+ a broad fallback)
        val scanFilters = listOf(
            ScanFilter.Builder().setServiceUuid(SERVICE_UUID).build(),
            ScanFilter.Builder().build()
        )
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bleScanner?.startScan(scanFilters, scanSettings, scanCallback)
        }

        handler.postDelayed({ stopBleScan() }, scanTimeout)
    }

    private fun stopBleScan() {
        if (isScanning && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                bleScanner?.stopScan(scanCallback)
            } catch (_: Exception) { /* ignore */ }
            isScanning = false
            Toast.makeText(this, "Scan finished", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val device = result?.device ?: return

            // Filter: show only PlantSensor devices or those advertising FFE0
            val nameOk = try {
                if (ActivityCompat.checkSelfPermission(this@SensorConnectionActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    device.name?.contains("PlantSensor", ignoreCase = true) == true
                } else false
            } catch (e: SecurityException) { false }

            val hasService = result.scanRecord?.serviceUuids?.any { it == SERVICE_UUID } == true

            if (!nameOk && !hasService) return

            if (!scanResults.contains(device)) {
                scanResults.add(device)
                updateDeviceList(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(this@SensorConnectionActivity, "Scan failed: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateDeviceList(device: BluetoothDevice) {
        val name = try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                device.name
            } else null
        } catch (e: SecurityException) {
            null
        }
        val displayText = "${name ?: device.address}\nTap to pair"
        binding.availableDevice.text = displayText
    }

    private fun simulateConnection(deviceName: String) {
        // keep your existing fake-pair flow; the real BLE connect runs in Dashboard via BLEManager
        binding.connectedDevice.text = "$deviceName (connected)"
        binding.availableDevice.text = "No other devices found"
        Toast.makeText(this, "Paired with $deviceName", Toast.LENGTH_SHORT).show()

        val prefs = getSharedPreferences("plant_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("isSetupDone", true).apply()

        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isScanning && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try { bleScanner?.stopScan(scanCallback) } catch (_: Exception) {}
        }
    }
}
