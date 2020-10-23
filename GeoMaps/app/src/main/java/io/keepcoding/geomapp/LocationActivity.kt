package io.keepcoding.geomapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import io.keepcoding.geomapp.FetchAddressIntentService.Companion.LOCATION_DATA_EXTRA
import io.keepcoding.geomapp.FetchAddressIntentService.Companion.RECEIVER
import io.keepcoding.geomapp.FetchAddressIntentService.Companion.RESULT_DATA_KEY
import io.keepcoding.geomapp.FetchAddressIntentService.Companion.SUCCESS_RESULT
import kotlinx.android.synthetic.main.activity_location.*

class LocationActivity : AppCompatActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34

    private val ADDRESS_REQUESTED_KEY = "address-request-pending"
    private val LOCATION_ADDRESS_KEY = "location-address"

    private var addressRequested = false

    private var addressOutput = ""

    // Creamos un cliente que nos ayudara en la obtencion de la localizacion
    private var fusedLocationClient: FusedLocationProviderClient? = null
    // Variable para la ultima localizacion
    private var lastLocation: Location? = null


    /**
     * LIFE CYCLE
     **/

    // Se crea la actividad
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Se crea la ventana en la que colocar la UI
        setContentView(R.layout.activity_location)

        // Configuramos elementos de la UI
        updateUIWidgets()
    }

    // La actividad es iniciada y se hace visible para el usuario
    public override fun onStart() {
        super.onStart()

        // Chequeamos permmisos de usuario a la geolocalizacion
        if (!checkPermissions()) {
            requestPermissions()
        } else {
            getAddress()
        }
    }


    /**
     * PRIVATE FUNCTIONS
     **/

    private fun updateUIWidgets() {
        // Alternamos la visibilidad de la barra de progreso y la operatividad del boton
        if (addressRequested) {
            progressBar.visibility = ProgressBar.VISIBLE
            fetchAddressButton.isEnabled = false
        } else {
            progressBar.visibility = ProgressBar.GONE
            fetchAddressButton.isEnabled = true
        }
    }

    private fun checkPermissions(): Boolean {
        // Obtenemos los permisos otorgados a la app en el Manifest
        val permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        // Devolvemos true solo si los permisos recuperados sean de permisos autorizados
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        // El metodo shouldShowRequestPermissionRationale devuelve true si debemos explicar al usuario la necesidad del permiso
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)

        if (shouldProvideRationale) {
            // Describimos por que la funcionalidad de la app necesita de los permisos
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                // Si el usuario acepta la solicitud de permisos se le requieren los mismos
                View.OnClickListener {
                    ActivityCompat.requestPermissions(this@LocationActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_PERMISSIONS_REQUEST_CODE)
                })

        } else {
            // No se explica al usuario de la necesidad de los permisos. Los solicitamos directamente
            ActivityCompat.requestPermissions(this@LocationActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getAddress() {
        // Obtenemos el valor del cliente
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        // Con el cliente obtenemos la lastLocation
        fusedLocationClient?.lastLocation?.addOnSuccessListener {
            lastLocation = lastLocation

            Snackbar.make(locationMainView, "Location ${lastLocation?.latitude}, ${lastLocation?.longitude}", Snackbar.LENGTH_LONG).show()
        }
    }


    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState ?: return

        with(savedInstanceState) {
            // Save whether the address has been requested.
            putBoolean(ADDRESS_REQUESTED_KEY, addressRequested)

            // Save the address string.
            putString(LOCATION_ADDRESS_KEY, addressOutput)
        }

        super.onSaveInstanceState(savedInstanceState)
    }

    /**
     * Receiver for data sent from FetchAddressIntentService.
     */
    private inner class AddressResultReceiver internal constructor(
        handler: Handler
    ) : ResultReceiver(handler) {

        /**
         * Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
         */
        override fun onReceiveResult(resultCode: Int, resultData: Bundle) {

            // Display the address string or an error message sent from the intent service.
            addressOutput = resultData.getString(RESULT_DATA_KEY)!!
            displayAddressOutput()

            // Show a toast message if an address was found.
            if (resultCode == SUCCESS_RESULT) {
                Toast.makeText(this@LocationActivity, R.string.address_found, Toast.LENGTH_SHORT).show()
            }

            // Reset. Enable the Fetch Address button and stop showing the progress bar.
            addressRequested = false
            updateUIWidgets()
        }
    }

    private fun showSnackbar(
        mainTextStringId: Int,
        actionStringId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(findViewById(android.R.id.content), getString(mainTextStringId),
            Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(actionStringId), listener)
            .show()
    }




    // Metodo que se ejecuta tras responder el usuario a la solicitud de permisos
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Comprobamos que el requestCode es el que hemos solicitado
        if (requestCode != REQUEST_PERMISSIONS_REQUEST_CODE) return
        // Evaluamos el resultado de la solicitud al usuario
        when {
            // Si la lista de resultados esta vacia el usuario salto o no acepto la solicitud
            grantResults.isEmpty() -> Log.i("", "User interaction was cancelled.")
            // Si la primera posicion de los resultados devuelve la aceptacion
            grantResults[0] == PackageManager.PERMISSION_GRANTED -> getAddress()
            // En el resto de casos indicamos la necesidad de los permisos en un SnackBar que posibilita acceder a Settings
            else -> showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                    View.OnClickListener {
                        // Construimos el Intent para pasar a la ventana de Settings
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            // URL de la de la pantalla de configuracion de permisos
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    })
        }

    }


    /**
     * Runs when user clicks the Fetch Address button.
     */
    @Suppress("UNUSED_PARAMETER")
    fun fetchAddressButtonHandler(view: View) {

    }

    private fun startIntentService() {
    }

    /**
     * Gets the address for the last known location.
     */


    /**
     * Updates the address in the UI.
     */
    private fun displayAddressOutput() {
        locationAddressView.text = addressOutput
    }
}