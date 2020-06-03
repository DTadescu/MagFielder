package shdv.example.magfielder.Utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.location.*
import shdv.example.magfielder.R
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

class GpsUtils(_locationManager: LocationManager) {
    var mLastLocation: Location? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private  val MY_PERMISSIONS_REQUEST_LOCATION = 99
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000
    private val locationManager = _locationManager
    private val mLocationRequest = LocationRequest()
    private var isLocationChanged = false
    private var isLocationGoing = false

    fun checkLocationPermissions(context: Context):Boolean{
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
            return false
        }
        return true

    }

    fun startLocationUpdates(context: Context) {

        // Create the location request to start receiving updates
        if(isLocationGoing) return
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest!!.setInterval(INTERVAL)
        mLocationRequest!!.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(context)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper())
        isLocationGoing = true
    }

    fun stoplocationUpdates() {

        if(mFusedLocationProviderClient != null)
            mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
        isLocationGoing = false
    }

    fun checkLocationProvider(context: Context):Boolean{
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps(context)
            return false
        }
        return true
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            mLastLocation = locationResult.lastLocation
        }
    }

    private fun buildAlertMessageNoGps(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage(context.getString(R.string.gps_seems_turnoff))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.yes)){dialog, id ->
                startActivityForResult(context as Activity, Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 11, null)
            }
            .setNegativeButton(context.getString(R.string.no)) { dialog, id ->
                dialog.cancel()

            }
        val alert: AlertDialog  = builder.create()
        alert.show()
    }

}