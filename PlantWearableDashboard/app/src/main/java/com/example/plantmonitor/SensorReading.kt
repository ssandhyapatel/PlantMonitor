package com.example.plantmonitor.data

data class SensorReading(
    val timestampMs: Long,
    val voc: Float,
    val temp: Float,
    val humidity: Float
)
