package com.example.plantmonitor.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.plantmonitor.R
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class AlertsActivity : AppCompatActivity() {

    private lateinit var alertRecyclerView: RecyclerView
    private lateinit var btnDismiss: Button
    private lateinit var btnExport: Button
    private lateinit var btnBack: ImageView
    private lateinit var adapter: AlertAdapter

    companion object {
        val recentAlerts = mutableListOf<AlertItem>()  // ✅ shared list for BLE alerts
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        alertRecyclerView = findViewById(R.id.alertRecyclerView)
        btnDismiss = findViewById(R.id.btnDismiss)
        btnExport = findViewById(R.id.btnExport)
        btnBack = findViewById(R.id.btnBack)

        adapter = AlertAdapter(recentAlerts)
        alertRecyclerView.layoutManager = LinearLayoutManager(this)
        alertRecyclerView.adapter = adapter

        // Optional: populate with mock if empty (for testing)
        if (recentAlerts.isEmpty()) loadMockAlerts()

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        btnDismiss.setOnClickListener {
            recentAlerts.clear()
            adapter.notifyDataSetChanged()
        }

        btnExport.setOnClickListener {
            exportToCSV()
        }
    }

    private fun loadMockAlerts() {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val now = formatter.format(Date())

        recentAlerts.add(AlertItem("VOC", "VOC Spike – 56 ppm", "at $now"))
        recentAlerts.add(AlertItem("TEMP", "Temp High – 37.2°C", "at $now"))
        recentAlerts.add(AlertItem("HUMIDITY", "Humidity Low – 38%", "at $now"))
        adapter.notifyDataSetChanged()
    }

    private fun exportToCSV() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                123
            )
            return
        }

        val csvFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "plant_alert_log.csv")
        try {
            val writer = FileWriter(csvFile)
            writer.append("Type,Message,Time\n")
            recentAlerts.forEach {
                writer.append("${it.type},${it.message},${it.time}\n")
            }
            writer.flush()
            writer.close()
            Toast.makeText(this, "Exported to ${csvFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            exportToCSV()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged() // refresh alerts when BLE sends new ones
    }
}

