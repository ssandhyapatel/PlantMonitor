package com.example.plantmonitor

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.plantmonitor.data.SensorDataStore
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TrendsActivity : AppCompatActivity() {

    private lateinit var lineChart: LineChart
    private lateinit var currentValue: TextView
    private lateinit var btnVoc: Button
    private lateinit var btnTemp: Button
    private lateinit var btnHumidity: Button
    private lateinit var btnBack: ImageView

    // ---- metric selection + live state ----
    private enum class Metric { VOC, TEMP, HUMIDITY }
    private var currentMetric: Metric = Metric.VOC

    // we only append new readings after this index (into SensorDataStore.readings)
    private var lastPlottedIndex: Int = 0

    // live refresh loop
    private val handler = Handler(Looper.getMainLooper())
    private val refreshIntervalMs = 1000L
    private val liveUpdater = object : Runnable {
        override fun run() {
            try {
                appendNewSamples()
            } finally {
                handler.postDelayed(this, refreshIntervalMs)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trends)

        lineChart = findViewById(R.id.lineChart)
        currentValue = findViewById(R.id.currentValue)
        btnVoc = findViewById(R.id.btnVoc)
        btnTemp = findViewById(R.id.btnTemp)
        btnHumidity = findViewById(R.id.btnHumidity)
        btnBack = findViewById(R.id.btnBack)

        initChart()
        rebuildDataFor(Metric.VOC)

        // keep your controls the same, upgrade behavior underneath
        btnVoc.setOnClickListener { rebuildDataFor(Metric.VOC) }
        btnTemp.setOnClickListener { rebuildDataFor(Metric.TEMP) }
        btnHumidity.setOnClickListener { rebuildDataFor(Metric.HUMIDITY) }
        btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
        handler.post(liveUpdater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(liveUpdater)
    }

    // ----------------------
    // Chart setup & styling
    // ----------------------
    private fun initChart() {
        // your description stays empty
        lineChart.description = Description().apply { text = "" }

        // interaction
        lineChart.setTouchEnabled(true)
        lineChart.setScaleEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.axisRight.isEnabled = false
        lineChart.setAutoScaleMinMaxEnabled(true)

        // X axis = time in seconds; we format to HH:mm:ss
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            isGranularityEnabled = true
            valueFormatter = TimeAxisValueFormatter()
            labelRotationAngle = -20f // helps avoid overlap
        }

        // Y axis grid stays for readability
        lineChart.axisLeft.setDrawGridLines(true)

        // legend
        lineChart.legend.apply {
            form = Legend.LegendForm.LINE
            isEnabled = true
        }

        // single dataset; we switch its label/values on metric change
        val ds = LineDataSet(emptyList(), "VOC Index").apply {
            lineWidth = 2f
            setDrawCircles(false)     // continuous line (no dots)
            setDrawValues(false)
            setDrawFilled(false)      // smoother for real-time
            mode = LineDataSet.Mode.LINEAR
        }
        lineChart.data = LineData(ds)
        lineChart.setNoDataText("Waiting for realtime data…")

        // show a time window (e.g., last 10 minutes)
        lineChart.setVisibleXRangeMaximum(10 * 60f) // seconds
    }

    // ---------------------------
    // Metric reload (full rebuild)
    // ---------------------------
    private fun rebuildDataFor(metric: Metric) {
        currentMetric = metric

        val readings = SensorDataStore.readings
        if (readings.isEmpty()) {
            // clear plot gracefully
            lineChart.data?.clearValues()
            lineChart.invalidate()
            currentValue.text = when (metric) {
                Metric.VOC -> "-- ppm"
                Metric.TEMP -> "-- °C"
                Metric.HUMIDITY -> "-- %"
            }
            lastPlottedIndex = 0
            return
        }

        // use a trailing window to seed the screen
        val start = (readings.size - 600).coerceAtLeast(0) // ~ last 10 min if ~1s cadence
        val slice = readings.subList(start, readings.size)

        val entries = ArrayList<Entry>(slice.size)
        for (r in slice) {
            val y = when (metric) {
                Metric.VOC -> r.voc
                Metric.TEMP -> r.temp
                Metric.HUMIDITY -> r.humidity
            }
            val xSec = r.timestampMs / 1000f
            entries.add(Entry(xSec, y))
        }

        val data = lineChart.data ?: LineData().also { lineChart.data = it }
        if (data.dataSetCount == 0) {
            val ds = LineDataSet(entries, metricLabel(metric)).apply {
                lineWidth = 2f
                setDrawCircles(false)
                setDrawValues(false)
                setDrawFilled(false)
                mode = LineDataSet.Mode.LINEAR
            }
            data.addDataSet(ds)
        } else {
            val ds = data.getDataSetByIndex(0) as LineDataSet
            ds.values = entries
            ds.label = metricLabel(metric)
            ds.setDrawCircles(false)
            ds.setDrawValues(false)
            ds.mode = LineDataSet.Mode.LINEAR
            ds.notifyDataSetChanged() // <- important when replacing values
        }

        // update current value from last entry
        val latest = entries.lastOrNull()?.y
        updateCurrentValue(latest, metric)

        data.notifyDataChanged()
        lineChart.notifyDataSetChanged()

        // move viewport to latest time
        val lastTsSec = (readings.last().timestampMs) / 1000f
        lineChart.moveViewToX(lastTsSec)
        lineChart.invalidate()

        // from now, append only new samples
        lastPlottedIndex = readings.size
    }

    private fun metricLabel(m: Metric): String = when (m) {
        Metric.VOC -> "VOC Index"
        Metric.TEMP -> "Temperature"
        Metric.HUMIDITY -> "Humidity"
    }

    private fun updateCurrentValue(latest: Float?, metric: Metric) {
        val unit = when (metric) {
            Metric.VOC -> "ppm"
            Metric.TEMP -> "°C"
            Metric.HUMIDITY -> "%"
        }
        val text = if (latest != null) {
            val shown = when (metric) {
                Metric.TEMP -> String.format(Locale.getDefault(), "%.1f", latest)
                else -> latest.toInt().toString()
            }
            "$shown $unit"
        } else {
            "-- $unit"
        }
        currentValue.text = text
    }

    // ---------------------------------------
    // Live append only NEW SensorData entries
    // ---------------------------------------
    private fun appendNewSamples() {
        val readings = SensorDataStore.readings
        if (readings.isEmpty()) return

        val data = lineChart.data ?: return
        if (data.dataSetCount == 0) {
            // dataset missing but we have readings -> rebuild once
            rebuildDataFor(currentMetric)
            return
        }
        val ds = data.getDataSetByIndex(0) as LineDataSet

        var appended = 0
        for (i in lastPlottedIndex until readings.size) {
            val r = readings[i]
            val y = when (currentMetric) {
                Metric.VOC -> r.voc
                Metric.TEMP -> r.temp
                Metric.HUMIDITY -> r.humidity
            }
            val xSec = r.timestampMs / 1000f
            val e = Entry(xSec, y)
            // Use data.addEntry so MPAndroidChart registers updates reliably
            data.addEntry(e, 0)
            appended++
        }

        if (appended > 0) {
            val lastEntry = (data.getDataSetByIndex(0) as LineDataSet).getEntryForIndex(
                (data.getDataSetByIndex(0) as LineDataSet).entryCount - 1
            )
            updateCurrentValue(lastEntry.y, currentMetric)

            data.notifyDataChanged()
            lineChart.notifyDataSetChanged()

            val lastTsSec = (readings.last().timestampMs) / 1000f
            lineChart.moveViewToX(lastTsSec)
            lineChart.invalidate()
        }

        lastPlottedIndex = readings.size
    }

    // -------------- helpers --------------
    private class TimeAxisValueFormatter : ValueFormatter() {
        private val df = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        override fun getFormattedValue(value: Float): String {
            val millis = (value * 1000L).toLong()
            return df.format(Date(millis))
        }
    }
}


