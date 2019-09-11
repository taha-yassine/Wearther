package com.tyassine.wearther

import org.json.JSONObject
import android.R
import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


object RemoteFetch {

//    private const val OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/weather?q=%s&units=metric"
//
//    fun getJSON(context: Context, city: String): JSONObject? {
//        try {
//            val url = URL(String.format(OPEN_WEATHER_MAP_API, city))
//            val connection = url.openConnection() as HttpURLConnection
//
//            connection.addRequestProperty(
//                "x-api-key",
//                context.getString(R.string.open_weather_maps_app_id)
//            )
//
//            val json = connection.inputStream.bufferedReader().use(BufferedReader::readText)
//
//            val data = JSONObject(json.toString())
//
//            // This value will be 404 if the request was not
//            // successful
//            return if (data.getInt("cod") != 200) {
//                null
//            } else data
//
//        } catch (e: Exception) {
//            return null
//        }
//
//    }
}