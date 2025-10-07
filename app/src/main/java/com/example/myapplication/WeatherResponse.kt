package com.example.myapplication

data class WeatherResponse(
    val main: Main
)

data class Main(
    val temp: Double,
    val humidity: Double
)
