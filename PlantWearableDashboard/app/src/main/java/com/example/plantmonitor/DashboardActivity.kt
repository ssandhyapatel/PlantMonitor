package com.example.plantmonitor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.example.plantmonitor.databinding.ActivityDashboardBinding
import com.example.plantmonitor.ui.AlertsActivity
import com.example.plantmonitor.TrendsActivity
import com.example.plantmonitor.ui.BLEManager
import com.example.plantmonitor.util.BluetoothPermissionsHelper
import com.google.android.material.bottomnavigation.BottomNavigationView

import kotlin.random.Random

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var bleManager: BLEManager
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!BluetoothPermissionsHelper.hasPermissions(this)) {
            BluetoothPermissionsHelper.requestPermissions(this, 101)
            return
        }

        setupBLE()
        simulateLiveSensorData()
        setupNavigation()
        setupBottomNav()
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    // Prevent restarting the same activity
                    if (this !is DashboardActivity) {
                        startActivity(Intent(this, DashboardActivity::class.java))
                    }
                    true
                }
                R.id.nav_trends -> {
                    startActivity(Intent(this, TrendsActivity::class.java))
                    true
                }
                R.id.nav_alerts -> {
                    startActivity(Intent(this, AlertsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun simulateLiveSensorData() {
        updateRunnable = object : Runnable {
            override fun run() {
                val voc = Random.nextInt(10, 60)
                val temp = Random.nextDouble(28.0, 38.0)
                val humidity = Random.nextInt(30, 80)
                com.example.plantmonitor.data.SensorDataStore.addReading(
                    voc.toFloat(),
                    temp.toFloat(),
                    humidity.toFloat()
                )

                binding.vocValue.text = getString(R.string.voc_value_format, voc)
                binding.tempValue.text = getString(R.string.temp_value_format, temp)
                binding.humidityValue.text = getString(R.string.humidity_value_format, humidity)

                val isStressed = voc > 50 || temp > 35 || humidity < 40
                binding.statusBox.setBackgroundColor(
                    if (isStressed) "#FF0000".toColorInt() else "#4CAF50".toColorInt()
                )
                binding.statusBox.setText(
                    if (isStressed) R.string.status_stressed else R.string.status_healthy
                )

                binding.lastUpdated.text = getString(R.string.updated_recently, Random.nextInt(1, 4))
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(updateRunnable)
    }

    private fun setupBLE() {
        bleManager = BLEManager(this) { voc, temp, hum ->
            runOnUiThread {
                com.example.plantmonitor.data.SensorDataStore.addReading(voc, temp, hum)

                binding.vocValue.text = getString(R.string.voc_value_format, voc.toInt())
                binding.tempValue.text = getString(R.string.temp_value_format, temp)
                binding.humidityValue.text = getString(R.string.humidity_value_format, hum.toInt())

                val isStressed = voc > 50 || temp > 35 || hum < 40
                binding.statusBox.setBackgroundColor(
                    if (isStressed) "#FF0000".toColorInt() else "#4CAF50".toColorInt()
                )
                binding.statusBox.setText(
                    if (isStressed) R.string.status_stressed else R.string.status_healthy
                )

                binding.lastUpdated.text = getString(R.string.last_updated_now)
            }
        }
        bleManager.startScan()
    }

    private fun setupNavigation() {
        binding.btnViewTrends.setOnClickListener {
            Toast.makeText(this, "Clicked Trends", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, TrendsActivity::class.java))
        }

        binding.btnPlantDetails.setOnClickListener {
            Toast.makeText(this, "Clicked Plant", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, PlantDetailsActivity::class.java))
        }

        binding.btnViewAlerts.setOnClickListener {
            Toast.makeText(this, "Clicked Alerts", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, AlertsActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setupBLE()
        }
    }
}
