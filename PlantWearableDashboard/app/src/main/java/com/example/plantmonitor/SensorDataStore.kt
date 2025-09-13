package com.example.plantmonitor.data

object SensorDataStore {

    // holds all readings in order of arrival
    val readings: MutableList<SensorReading> = mutableListOf()

    /**
     * Adds a new reading with the current timestamp.
     */
    fun addReading(voc: Float, temp: Float, humidity: Float) {
        readings.add(
            SensorReading(
                timestampMs = System.currentTimeMillis(),
                voc = voc,
                temp = temp,
                humidity = humidity
            )
        )
        // optional: cap the list to avoid unbounded growth
        if (readings.size > 1000) readings.removeAt(0)
    }

    /** Optional: clear all readings. */
    fun clear() = readings.clear()
}



