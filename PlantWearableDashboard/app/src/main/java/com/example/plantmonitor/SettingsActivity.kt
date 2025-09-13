package com.example.plantmonitor

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.plantmonitor.databinding.ActivitySettingsBinding
import com.example.plantmonitor.ui.AlertsActivity


class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val syncOptions = arrayOf("5s", "10s", "30s", "60s")
    private val plantTypes = arrayOf("Rubber", "Money Plant", "Aloe Vera", "Tulsi", "Snake Plant", "Spider Plant")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // VOC Threshold
        binding.vocValue.text = binding.seekVoc.progress.toString()
        binding.seekVoc.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.vocValue.text = progress.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Dropdowns
        binding.syncValue.setOnClickListener {
            showDropdownDialog(binding.syncValue, syncOptions)
        }
        binding.plantValue.setOnClickListener {
            showDropdownDialog(binding.plantValue, plantTypes)
        }

        // ✅ Bottom Navigation Setup
        binding.bottomNavigationView.selectedItemId = R.id.nav_settings
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
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
                R.id.nav_settings -> true // Already on settings
                else -> false
            }
        }
    }

    private fun showDropdownDialog(targetView: TextView, options: Array<String>) {
        val builder = AlertDialog.Builder(this)
        builder.setItems(options) { _, which ->
            targetView.text = options[which]
        }
        builder.show()
    }
}

