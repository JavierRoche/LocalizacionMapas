package io.keepcoding.geomapp

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.ResultReceiver
import java.io.IOException
import java.util.Locale

class FetchAddressIntentService : IntentService("FetchAddress") {

    companion object {
        const val SUCCESS_RESULT = 0

        const val FAILURE_RESULT = 1

        private const val PACKAGE_NAME = "com.google.android.gms.location.sample.locationaddress"

        const val RECEIVER = "$PACKAGE_NAME.RECEIVER"

        const val RESULT_DATA_KEY = "$PACKAGE_NAME.RESULT_DATA_KEY"

        const val LOCATION_DATA_EXTRA = "$PACKAGE_NAME.LOCATION_DATA_EXTRA"
    }

    private var receiver: ResultReceiver? = null

    override fun onHandleIntent(intent: Intent?) {

    }

    private fun deliverResultToReceiver(resultCode: Int, message: String) {
        val bundle = Bundle().apply { putString(RESULT_DATA_KEY, message) }
        receiver?.send(resultCode, bundle)
    }

}