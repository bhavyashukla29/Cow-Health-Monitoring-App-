package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import org.json.JSONArray
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var tempInput: EditText
    private lateinit var humidityInput: EditText
    private lateinit var calcBtn: Button
    private lateinit var manualResult: TextView
    private lateinit var autoTHI: TextView
    private lateinit var refreshBtn: Button
    private lateinit var thiStatusLabel: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var trendsBtn: Button

    private val apiKey = "77530b8688279ecaf046b0ce6b141dcb"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        tempInput = findViewById(R.id.tempInput)
        humidityInput = findViewById(R.id.humidityInput)
        calcBtn = findViewById(R.id.calcBtn)
        manualResult = findViewById(R.id.manualResult)
        autoTHI = findViewById(R.id.autoTHI)
        refreshBtn = findViewById(R.id.refreshBtn)
        thiStatusLabel = findViewById(R.id.thiStatusLabel)
        progressBar = findViewById(R.id.thiProgressBar)
        trendsBtn = findViewById(R.id.trendsBtn)

        calcBtn.setOnClickListener {
            val temp = tempInput.text.toString().toDoubleOrNull()
            val rh = humidityInput.text.toString().toDoubleOrNull()

            if (temp == null || rh == null) {
                manualResult.text = "Please enter valid numbers"
                return@setOnClickListener
            }

            val thi = calculateTHI(temp, rh / 100.0).roundToInt()
            manualResult.text = "THI: $thi"
            updateTHIUI(thi)
        }

        refreshBtn.setOnClickListener {
            requestLocationAndFetchWeather()
        }

        trendsBtn.setOnClickListener {
            startActivity(Intent(this, TrendsActivity::class.java))
        }

        requestLocationAndFetchWeather()
    }

    private fun calculateTHI(temp: Double, rh: Double): Double {
        return 0.8 * temp + rh * (temp - 14.4) + 46.4
    }

    private fun requestLocationAndFetchWeather() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            numUpdates = 1
            interval = 0
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    fetchWeather(location.latitude, location.longitude)
                } else {
                    autoTHI.text = "Location still not available"
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == 101 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationAndFetchWeather()
        } else {
            autoTHI.text = "Location permission denied"
        }
    }

    private fun fetchWeather(lat: Double, lon: Double) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(WeatherApiService::class.java)
        val call = api.getWeatherByCoordinates(lat, lon, apiKey)

        call.enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                if (response.isSuccessful) {
                    val weather = response.body()
                    val temp = weather?.main?.temp ?: 0.0
                    val rh = weather?.main?.humidity ?: 0.0
                    val thi = calculateTHI(temp, rh / 100.0).roundToInt()

                    autoTHI.text = "Live THI: $thi (Temp: $temp°C, RH: $rh%)"
                    updateTHIUI(thi)
                } else {
                    autoTHI.text = "Weather error: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                autoTHI.text = "API error: ${t.message}"
            }
        })
    }

    private fun updateTHIUI(thi: Int) {
        progressBar.progress = thi
        thiStatusLabel.text = "THI Status: ${getTHIStatusLabel(thi)}"

        saveTHIHistory(thi)
    }

    private fun saveTHIHistory(thi: Int) {
        val prefs = getSharedPreferences("THI_HISTORY", Context.MODE_PRIVATE)
        val jsonString = prefs.getString("history", "[]") ?: "[]"

        val jsonArray = try {
            JSONArray(jsonString)
        } catch (e: Exception) {
            JSONArray()
        }

        jsonArray.put(thi)

        if (jsonArray.length() > 7) {
            val start = jsonArray.length() - 7
            val trimmed = JSONArray()
            for (i in start until jsonArray.length()) {
                trimmed.put(jsonArray.getInt(i))
            }
            prefs.edit().putString("history", trimmed.toString()).apply()
        } else {
            prefs.edit().putString("history", jsonArray.toString()).apply()
        }
    }

    private fun getTHIStatusLabel(thi: Int): String {
        return when {
            thi < 68 -> "Safe"
            thi in 68..73 -> "Mild Stress"
            thi in 74..79 -> "Moderate Stress"
            thi in 80..84 -> "Severe Stress"
            else -> "Dangerous"
        }
    }
}
