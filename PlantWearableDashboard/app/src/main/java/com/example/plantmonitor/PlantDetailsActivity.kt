package com.example.plantmonitor

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PlantDetailsActivity : AppCompatActivity() {

    private lateinit var plantNameText: TextView
    private lateinit var plantImage: ImageView
    private lateinit var vocStatus: TextView
    private lateinit var lightLevel: TextView
    private lateinit var recommendationText: TextView
    private lateinit var removeButton: Button

    // Mock sensor data – replace these with real values later
    private val vocLevel = 70   // in ppm
    private val luxLevel = 120  // in lux
    private val humidity = 35   // in %

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_details)

        // Bind views
        plantNameText = findViewById(R.id.plantName)
        plantImage = findViewById(R.id.plantImage)
        vocStatus = findViewById(R.id.vocStatus)
        lightLevel = findViewById(R.id.lightLevel)
        recommendationText = findViewById(R.id.recommendationText)
        removeButton = findViewById(R.id.removePlantBtn)

        // Set static content
        plantNameText.text = "Rubber"
        plantImage.setImageResource(R.drawable.rubber) // Ensure drawable exists

        // Update sensor-related UI
        vocStatus.text = getVocStatus(vocLevel)
        lightLevel.text = getLightStatus(luxLevel)
        recommendationText.text = generateRecommendation(vocLevel, luxLevel, humidity)

        removeButton.setOnClickListener {
            finish() // Close activity or handle delete
        }
    }

    private fun getVocStatus(voc: Int): String {
        return when {
            voc < 50 -> "VOC: Optimal"
            voc in 50..100 -> "VOC: Moderate"
            else -> "VOC: Poor"
        }
    }

    private fun getLightStatus(lux: Int): String {
        return when {
            lux < 100 -> "Light level: Low"
            lux in 100..400 -> "Light level: Moderate"
            else -> "Light level: High"
        }
    }

    private fun generateRecommendation(voc: Int, light: Int, humidity: Int): String {
        val advice = StringBuilder()

        if (voc > 100) advice.append("Improve air circulation. ")
        if (light < 100) advice.append("Place the plant in a brighter spot. ")
        if (humidity < 40) advice.append("Humidity is low. Mist the plant. ")
        if (advice.isEmpty()) advice.append("All parameters are within optimal range. Keep it up!")

        return advice.toString()
    }
}
