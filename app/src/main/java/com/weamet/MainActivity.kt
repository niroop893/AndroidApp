// File: MainActivity.kt

package com.weamet

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cityInput: EditText = findViewById(R.id.cityInput)
        val fetchButton: Button = findViewById(R.id.fetchButton)
        val weatherReport: TextView = findViewById(R.id.weatherReport)
        progressBar = findViewById(R.id.progressBar)

        fetchButton.setOnClickListener {
            val cityName = cityInput.text.toString().trim()
            if (cityName.isNotEmpty()) {
                fetchWeatherData(cityName, weatherReport)
            } else {
                weatherReport.text = "Please enter a city name"
            }
        }
    }

    private fun fetchWeatherData(cityName: String, weatherReport: TextView) {
        progressBar.visibility = View.VISIBLE // Show progress bar
        weatherReport.text = "" // Clear previous text

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: Geocode the city
                val geocodeUrl =
                    "https://geocoding-api.open-meteo.com/v1/search?name=$cityName&count=1"
                val geocodeResponse = fetchFromUrl(geocodeUrl)
                val geoJson = JSONObject(geocodeResponse)
                val results = geoJson.optJSONArray("results")

                if (results == null || results.length() == 0) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        weatherReport.text = "City not found in geocoding service."
                    }
                    return@launch
                }

                val firstResult = results.getJSONObject(0)
                val latitude = firstResult.optDouble("latitude", Double.NaN)
                val longitude = firstResult.optDouble("longitude", Double.NaN)

                if (latitude.isNaN() || longitude.isNaN()) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        weatherReport.text = "Invalid location coordinates."
                    }
                    return@launch
                }

                // Step 2: Fetch weather data
                val weatherUrl =
                    "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current_weather=true&daily=precipitation_sum&timezone=auto"
                val weatherResponse = fetchFromUrl(weatherUrl)
                val weatherJson = JSONObject(weatherResponse)
                val currentWeather = weatherJson.optJSONObject("current_weather")
                val dailyWeather = weatherJson.optJSONObject("daily")

                if (currentWeather == null) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        weatherReport.text = "Weather data not available."
                    }
                    return@launch
                }

                val temperature = currentWeather.optDouble("temperature", Double.NaN)
                val windSpeed = currentWeather.optDouble("windspeed", Double.NaN)
                val humidity = currentWeather.optDouble("relative_humidity", Double.NaN) // Hypothetical API field
                val precipitation = dailyWeather?.optJSONArray("precipitation_sum")?.optDouble(0, Double.NaN)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    weatherReport.text = buildString {
                        append("City: $cityName\n")
                        if (!temperature.isNaN()) append("Temperature: $temperature°C\n")
                        else append("Temperature: Data not available\n")
                        if (!windSpeed.isNaN()) append("Wind Speed: $windSpeed km/h\n")
                        else append("Wind Speed: Data not available\n")
                        if (!humidity.isNaN()) append("Humidity: $humidity%\n")
                        else append("Humidity: Data not available\n")
                        if (precipitation != null && !precipitation.isNaN()) append("Precipitation: $precipitation mm\n")
                        else append("Precipitation: Data not available\n")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    weatherReport.text = "Error fetching weather data: ${e.message}"
                }
            }
        }
    }

    private fun fetchFromUrl(url: String): String {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Failed to fetch data: ${response.code}")
            return response.body?.string() ?: throw Exception("Empty response")
        }
    }
}
