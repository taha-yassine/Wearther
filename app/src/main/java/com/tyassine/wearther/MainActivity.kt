package com.tyassine.wearther

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.ImageViewCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.tyassine.wearther.Data.Weather
import com.tyassine.wearther.Notification.AlarmReceiver
import com.tyassine.wearther.Services.Constants
import com.tyassine.wearther.Services.FetchCityIntentService

import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder


const val PERMISSIONS_REQUEST_LOCATION = 0
const val API_URL = "https://api.openweathermap.org/data/2.5/weather?"
const val API_KEY = "67b70e1a0803a476dfa108fa178a3076"
const val CHANNEL_ID = "notification_channel"
const val DAILY_REMINDER_REQUEST_CODE = 42
const val NOTIFICATION_DATE = 8
const val DEFAULT_LAT = 54.972972972972975
const val DEFAULT_LONG = -1.632400178839545

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var lastLocation: Location? = null
    private lateinit var resultReceiver: AddressResultReceiver

    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val rvData = mutableListOf<String>()

    private lateinit var sharedPreferences: SharedPreferences

    private var autoLocation = true



    class RecomAdapter(private val recomDataset: List<String>) : RecyclerView.Adapter<RecomAdapter.RecomViewHolder>() {
        class RecomViewHolder(val imgView: ImageView) : RecyclerView.ViewHolder(imgView)

        // Create new views (invoked by the layout manager)
        override fun onCreateViewHolder(parent: ViewGroup,
                                        viewType: Int): RecomAdapter.RecomViewHolder {
            // create a new view
            val imgView = ImageView(parent.context)
            imgView.setPadding(32,64,32,64)
            imgView.adjustViewBounds = true

            return RecomViewHolder(imgView)
        }

        // Replace the contents of a view (invoked by the layout manager)
        override fun onBindViewHolder(holder: RecomViewHolder, position: Int) {
            val c = holder.imgView.context
            val resourceID = c.resources.getIdentifier("recom_${recomDataset[position]}", "drawable", c.packageName)
            val resource = ResourcesCompat.getDrawable(c.resources, resourceID, null)
            holder.imgView.setImageDrawable(resource)

            holder.imgView.setOnClickListener { v ->
                val escapedQuery = URLEncoder.encode(recomDataset[position].replace("_", " "), "UTF-8")
                val uri = Uri.parse("http://www.google.com/#q=" + escapedQuery)
                val  intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(v.context, intent, null)
                //Toast.makeText(c, recomDataset[position], Toast.LENGTH_SHORT).show()
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        override fun getItemCount() = recomDataset.size
    }

    //Receiver for the FetchCityIntentService
    internal inner class AddressResultReceiver(handler: Handler) : ResultReceiver(handler) {

        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {

            // Display the address string
            // or an error message sent from the intent service.

            // Show a toast message if an address was found.
            if (resultCode == Constants.SUCCESS_RESULT) {
                //updateLocation(resultData?.getString(Constants.RESULT_DATA_KEY) ?: "")
            }

        }
    }

    //Task in charge of retrieving weather information
    inner class RetrieveWeatherTask : AsyncTask<String, Void, Weather>() {
        override fun doInBackground(vararg params: String?): Weather {
            val unit = if (sharedPreferences.getString("unit", "0") == "0") "metric" else "imperial"
            val url = URL("${API_URL}lat=${params[0]}&lon=${params[1]}&appid=$API_KEY&units=$unit")
            val urlConnection = url.openConnection() as HttpURLConnection
            val data = urlConnection.inputStream.bufferedReader().readText()
            return parseJSON(data)
        }

        private fun parseJSON(json: String): Weather {

            var temp = 0
            var min = 0
            var max = 0
            var desc = ""
            var location = ""

            try {
                val obj = JSONObject(json)
                temp = obj.getJSONObject("main").getInt("temp")
                min = obj.getJSONObject("main").getInt("temp_min")
                max = obj.getJSONObject("main").getInt("temp_max")
                desc = obj.getJSONArray("weather").getJSONObject(0).getString("main")
                location = obj.getString("name")+", "+obj.getJSONObject("sys").getString("country")

            } catch (e: JSONException) {
                // Appropriate error handling code
            } catch (nfe: NumberFormatException) {
                // not a valid int
            }

            return Weather(temp, min, max, desc, location)
        }

        override fun onPostExecute(result: Weather?) {
            super.onPostExecute(result)

            //Retrieve preferences
            val unit = if (sharedPreferences.getString("unit", "0") == "0") "°C" else "°F"

            temperature.text = result?.temp.toString()+unit
            description.text = result?.desc
            min.text = result?.min.toString()+unit
            max.text = result?.max.toString()+unit

            location.text = result?.location

            val jsonTxt = resources.openRawResource(R.raw.recom_data).bufferedReader().readText()
            val obj = JSONObject(jsonTxt)
            val recomsUnisex = obj.getJSONObject("weather").getJSONObject(result?.desc).getJSONArray("unisex")

            val gender = if (sharedPreferences.getString("gender", "0") == "0") "male" else "female"
            val recomsGender = obj.getJSONObject("weather").getJSONObject(result?.desc).getJSONArray(gender)

            rvData.clear()
            for (i in 0 until recomsGender.length()) {
                rvData.add(recomsGender.getString(i))
            }
            for (i in 0 until recomsUnisex.length()) {
                rvData.add(recomsUnisex.getString(i))
            }
            viewAdapter.notifyDataSetChanged()

            val backgroundGradient = obj.getJSONObject("weather").getJSONObject(result?.desc).getString("background")
            mainLayout.setBackgroundResource(mainLayout.context.resources.getIdentifier(backgroundGradient, "drawable",
                mainLayout.context.packageName
            ))

            icon.text = obj.getJSONObject("weather").getJSONObject(result?.desc).getString("icon")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this )
        //Preference change management
        val listener: SharedPreferences.OnSharedPreferenceChangeListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                configureFromPreferences(key)
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        resultReceiver = AddressResultReceiver(Handler())

        //Get user's location preferences
        configureFromPreferences("auto_location")

        //Update current user's location
        updateLocation(autoLocation)

        //Recycler View for the recommendations section
        viewManager = GridLayoutManager(this, 3)
        viewAdapter = RecomAdapter(rvData)

        recom_RV.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        //Create notification channel
        createNotificationChannel()

        //Set reminder
        configureFromPreferences("notifications")


    }

    //Gets current user's location or custom location and converts it to a city
    private fun updateLocation(autoLocation: Boolean) {
        if (autoLocation) {
            //Location permission handling
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                != PackageManager.PERMISSION_GRANTED
            ) {

                // Permission is not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    PERMISSIONS_REQUEST_LOCATION
                )
            } else {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->

                        lastLocation = location
                        // Get last known location
                        Log.d("RESULTS", "${location?.latitude} ; ${location?.longitude}")
                        if (lastLocation == null) return@addOnSuccessListener

                        //Reverse geocoding the location into a city
                        if (!Geocoder.isPresent()) {
                            return@addOnSuccessListener
                        }

                        //Start weather task
                        RetrieveWeatherTask().execute(
                            lastLocation?.latitude.toString(),
                            lastLocation?.longitude.toString()
                        )

                        // Start reverse geocoding service
                        this@MainActivity.startIntentService()
                    }
            }
        }
        else {
            //Start weather task
            RetrieveWeatherTask().execute(
                DEFAULT_LAT.toString(),
                DEFAULT_LONG.toString()
            )
            lastLocation = Location("")
            lastLocation!!.latitude = DEFAULT_LAT
            lastLocation!!.longitude = DEFAULT_LONG

            // Start reverse geocoding service
            this@MainActivity.startIntentService()
        }

    }

    //When the user responds to permission request
    override fun onRequestPermissionsResult(requestCode: Int,
                                           permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    updateLocation(autoLocation)
                } else {
                    // permission denied
                }
                return
            }

            else -> {
                // Ignore all other requests
            }
        }
    }

    //Starts the reverse geocoding intent service
    private fun startIntentService() {

        val intent = Intent(this, FetchCityIntentService::class.java).apply {
            putExtra(Constants.RECEIVER, resultReceiver)
            putExtra(Constants.LOCATION_DATA_EXTRA, lastLocation)
        }
        startService(intent)
    }

    //Preferences button
    fun preferencesButton(view: View) {
        val intent = Intent(this, PreferencesActivity::class.java)
        startActivity(intent)
    }

    fun refreshButton(view: View) {
        updateLocation(autoLocation)
    }

    //Create the NotificationChannel
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    //Set the repeating notification
    @SuppressLint("NewApi")
    fun setReminder(context: Context, cls: Class<*>, hour: Int, min: Int) {
        val calendar = Calendar.getInstance()
        val setcalendar = Calendar.getInstance()
        setcalendar.set(Calendar.HOUR_OF_DAY, hour)
        setcalendar.set(Calendar.MINUTE, min)
        setcalendar.set(Calendar.SECOND, 0)
        // cancel already scheduled reminders
        removeReminder(context, cls)

        if (setcalendar.before(calendar))
            setcalendar.add(Calendar.DATE, 1)

        // Enable a receiver
        val receiver = ComponentName(context, cls)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        val intent1 = Intent(context, cls)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE, intent1,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP, setcalendar.timeInMillis,
            AlarmManager.INTERVAL_DAY, pendingIntent
        )
    }

    //Cancel the repeating notification
    private fun removeReminder(context: Context, cls: Class<*>) {
        // Disable a receiver
        val receiver = ComponentName(context, cls)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        val intent1 = Intent(context, cls)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE, intent1, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    //Retrieves the preference corresponding to the key and applies changes to the app
    private fun configureFromPreferences (key: String) {
        when(key) {
            "notifications" -> {
                if (sharedPreferences.getBoolean(key, true)) {
                    setReminder(this, AlarmReceiver::class.java, NOTIFICATION_DATE, 0)
                }
                else removeReminder(this, AlarmReceiver::class.java)
            }
            "unit", "gender" -> updateLocation(autoLocation)
            "auto_location" -> autoLocation = sharedPreferences.getBoolean(key, true)
            else -> {}
        }
    }

}
