package com.tyassine.wearther.Services

import android.app.IntentService
import android.content.ContentValues.TAG
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.ResultReceiver
import android.util.Log
import com.tyassine.wearther.R
import java.io.IOException
import java.util.*

object Constants {
    const val SUCCESS_RESULT = 0
    const val FAILURE_RESULT = 1
    const val PACKAGE_NAME = "com.tyassine.wearther"
    const val RECEIVER = "$PACKAGE_NAME.RECEIVER"
    const val RESULT_DATA_KEY = "${PACKAGE_NAME}.RESULT_DATA_KEY"
    const val LOCATION_DATA_EXTRA = "${PACKAGE_NAME}.LOCATION_DATA_EXTRA"
}

class FetchCityIntentService: IntentService("FetchCityIntentService") {

    private var receiver: ResultReceiver? = null

    override fun onHandleIntent(intent: Intent?) {

        intent ?: return

        receiver = intent.getParcelableExtra(Constants.RECEIVER)

        var errorMessage = ""

        // Get the location passed to this service through an extra
        val location: Location = intent.getParcelableExtra(
            Constants.LOCATION_DATA_EXTRA)

        val geocoder = Geocoder(this, Locale.getDefault())

        var addresses: List<Address> = emptyList()


        try {
            addresses = geocoder.getFromLocation(
                location.latitude,
                location.longitude,
                1)
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available)
            Log.e(TAG, errorMessage, ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used)
            Log.e(TAG, "$errorMessage. Latitude = ${location.latitude} , " +
                    "Longitude =  ${location.longitude}", illegalArgumentException)
        }

        // Handle case where no address was found.
        if (addresses.isEmpty()) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found)
                Log.e(TAG, errorMessage)
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage)
        } else {
            val address = addresses[0]
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                "${address.locality}, ${address.adminArea}")
        }
    }

    private fun deliverResultToReceiver(resultCode: Int, message: String) {
        val bundle = Bundle().apply { putString(Constants.RESULT_DATA_KEY, message) }
        receiver?.send(resultCode, bundle)
    }
}