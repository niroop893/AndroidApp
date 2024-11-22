package com.weamet

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.airbnb.lottie.LottieAnimationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Switch
import android.widget.ProgressBar
import com.airbnb.lottie.LottieDrawable
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var weatherAnimation1: LottieAnimationView
    private lateinit var weatherAnimation2: LottieAnimationView
    private lateinit var weatherAnimation3: LottieAnimationView
    private lateinit var progressBar: ProgressBar
    private lateinit var weatherIcon: ImageView  // Add this line
    private lateinit var sharedPreferences: SharedPreferences
    private var isFahrenheit: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        weatherIcon = findViewById(R.id.weatherIcon)

        weatherAnimation1 = findViewById(R.id.weatherAnimation1)
        weatherAnimation2 = findViewById(R.id.weatherAnimation2)
        weatherAnimation3 = findViewById(R.id.weatherAnimation3)

        val cityInput: EditText = findViewById(R.id.cityInput)
        val fetchButton: Button = findViewById(R.id.fetchButton)
        val weatherReport: TextView = findViewById(R.id.weatherReport)
        val unitSwitch: Switch = findViewById(R.id.unitSwitch)
        progressBar = findViewById(R.id.progressBar)

        setupAnimations()

        fetchButton.setOnClickListener {
            val city = cityInput.text.toString()
            if (city.isNotEmpty()) {
                fetchWeatherData(city, weatherReport)
            }
        }

        unitSwitch.setOnCheckedChangeListener { _, isChecked ->
            isFahrenheit = isChecked
        }
    }

    private fun setupAnimations() {
        weatherAnimation1.apply {
            speed = 1f
            repeatCount = LottieDrawable.INFINITE
            visibility = View.GONE
        }

        weatherAnimation2.apply {
            speed = 1f
            repeatCount = LottieDrawable.INFINITE
            visibility = View.GONE
        }

        weatherAnimation3.apply {
            speed = 1f
            repeatCount = LottieDrawable.INFINITE
            visibility = View.GONE
        }
    }

    private fun updateWeatherAnimation(weatherCode: Int) {
        // Reset all animations
        weatherAnimation1.visibility = View.GONE
        weatherAnimation2.visibility = View.GONE
        weatherAnimation3.visibility = View.GONE

        // Map weather codes to specific animations and icons
        when (weatherCode) {
            // Clear sky
            0 -> {
                weatherAnimation1.visibility = View.VISIBLE
                weatherAnimation1.playAnimation()
                weatherIcon.setImageResource(R.drawable.sunny)
            }
            // Partly cloudy, cloudy
            1, 2, 3 -> {
                weatherAnimation2.visibility = View.VISIBLE
                weatherAnimation2.playAnimation()
                weatherIcon.setImageResource(R.drawable.cloudy)
            }
            // Rain
            61, 63, 65, 80, 81, 82 -> {
                weatherAnimation3.visibility = View.VISIBLE
                weatherAnimation3.playAnimation()
                weatherIcon.setImageResource(R.drawable.rainy)
            }
            // Thunder
            95, 96, 99 -> {
                weatherAnimation3.visibility = View.VISIBLE
                weatherAnimation3.playAnimation()
                weatherIcon.setImageResource(R.drawable.thunder)
            }
            // Snow
            71, 73, 75, 77, 85, 86 -> {
                weatherAnimation2.visibility = View.VISIBLE
                weatherAnimation2.playAnimation()
                weatherIcon.setImageResource(R.drawable.snowy)
            }
            // Default weather
            else -> {
                weatherAnimation1.visibility = View.VISIBLE
                weatherAnimation1.playAnimation()
                weatherIcon.setImageResource(R.drawable.default_weather)
            }
        }
    }



// ... (setupAnimations and updateWeatherAnimation remain the same)

    private fun fetchWeatherData(cityName: String, weatherReport: TextView) {
        progressBar.visibility = View.VISIBLE
        weatherReport.text = ""

        weatherAnimation1.cancelAnimation()
        weatherAnimation2.cancelAnimation()
        weatherAnimation3.cancelAnimation()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()

                // Geocoding request
                val geocodeUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$cityName&count=1&language=en&format=json"
                val geocodeRequest = Request.Builder().url(geocodeUrl).build()
                val geocodeResponse = client.newCall(geocodeRequest).execute()
                val geocodeJson = JSONObject(geocodeResponse.body?.string() ?: "")

                val results = geocodeJson.getJSONArray("results")
                if (results.length() > 0) {
                    val location = results.getJSONObject(0)
                    val lat = location.getDouble("latitude")
                    val lon = location.getDouble("longitude")

                    // Weather request
                    val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weathercode,windspeed_10m,precipitation&temperature_unit=${if(isFahrenheit) "fahrenheit" else "celsius"}"
                    val weatherRequest = Request.Builder().url(weatherUrl).build()
                    val weatherResponse = client.newCall(weatherRequest).execute()
                    val weatherJson = JSONObject(weatherResponse.body?.string() ?: "")

                    val current = weatherJson.getJSONObject("current")
                    val temperature = current.getDouble("temperature_2m")
                    val windSpeed = current.getDouble("windspeed_10m")
                    val precipitation = current.getDouble("precipitation")
                    val weatherCode = current.getInt("weathercode")

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        updateWeatherAnimation(weatherCode)
                        weatherReport.text = buildString {
                            append("City: $cityName\n")
                            append("Temperature: $temperature ${if (isFahrenheit) "°F" else "°C"}\n")
                            append("Wind Speed: $windSpeed km/h\n")
                            append("Precipitation: $precipitation mm\n")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        weatherReport.text = "City not found"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    weatherAnimation1.visibility = View.GONE
                    weatherAnimation2.visibility = View.GONE
                    weatherAnimation3.visibility = View.GONE
                    weatherReport.text = "Error fetching weather data: ${e.message}"
                }
            }
        }
    }
}
