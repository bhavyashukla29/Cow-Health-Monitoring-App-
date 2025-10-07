package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import org.json.JSONArray

class TrendsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trends)

        val lineChart = findViewById<LineChart>(R.id.lineChart)

        // Load last 7 saved THI values
        val sharedPref = getSharedPreferences("THI_HISTORY", MODE_PRIVATE)
        val historyJson = sharedPref.getString("history", "[]") ?: "[]"
        val jsonArray = JSONArray(historyJson)

        val thiValues = ArrayList<Float>()
        for (i in 0 until jsonArray.length()) {
            thiValues.add(jsonArray.getInt(i).toFloat())
        }

        val entries = ArrayList<Entry>()
        thiValues.forEachIndexed { index, value ->
            entries.add(Entry(index.toFloat(), value))
        }

        val dataSet = LineDataSet(entries, "THI Values (Last 7 Days)").apply {
            color = Color.BLACK                   // Line = Black
            valueTextColor = Color.BLACK          // Numbers = Black
            lineWidth = 3f
            circleRadius = 5f
            setCircleColor(Color.BLACK)           // Dots = Black
            highLightColor = Color.BLACK          // Highlight = Black
            setDrawFilled(false)
            setDrawValues(true)
        }

        lineChart.data = LineData(dataSet)

        // X-axis (Day numbers)
        lineChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            textSize = 12f
            textColor = Color.BLACK
        }

        // Y-axis (THI scale)
        lineChart.axisLeft.apply {
            axisMinimum = 40f
            axisMaximum = 100f
            granularity = 5f
            textSize = 12f
            textColor = Color.BLACK
        }

        // Right Y-axis disabled
        lineChart.axisRight.isEnabled = false

        // Hide description + legend
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = false

        lineChart.invalidate()
    }
}
