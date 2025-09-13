package com.example.plantmonitor.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.plantmonitor.R

data class AlertItem(
    val type: String, // "VOC", "TEMP", "HUMIDITY"
    val message: String,
    val time: String
)

class AlertAdapter(private val alerts: List<AlertItem>) : RecyclerView.Adapter<AlertAdapter.AlertViewHolder>() {

    class AlertViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: View = itemView.findViewById(R.id.alertIcon)
        val title: TextView = itemView.findViewById(R.id.alertTitle)
        val time: TextView = itemView.findViewById(R.id.alertTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false)
        return AlertViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.title.text = alert.message
        holder.time.text = alert.time

        // Set icon color based on type
        val color = when (alert.type.uppercase()) {
            "VOC" -> Color.RED
            "TEMP" -> Color.parseColor("#FF9800") // Orange
            "HUMIDITY" -> Color.parseColor("#FFC107") // Yellow
            else -> Color.GRAY
        }
        holder.icon.setBackgroundColor(color)
    }

    override fun getItemCount(): Int = alerts.size
}
