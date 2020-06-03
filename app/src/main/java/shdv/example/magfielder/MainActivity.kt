package shdv.example.magfielder

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.google.android.gms.location.*
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import shdv.example.magfielder.Utils.*
import java.time.Year
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mGpsUtils:GpsUtils

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val INTERVAL: Long = 2000
    private val FASTEST_INTERVAL: Long = 1000
    lateinit var mLastLocation: Location
    internal lateinit var mLocationRequest: LocationRequest


    //test
    private lateinit var sPref: SharedPreferences

    private  val MY_PERMISSIONS_REQUEST_LOCATION = 99
    private var isLocationChanged = false
    private var isLocationGoing = false

    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        //if(!isInit) initSettings()
        //runBlocking { delay(500) }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        mLocationRequest = LocationRequest()
        mGpsUtils = GpsUtils(getSystemService(LOCATION_SERVICE) as LocationManager)
        setlocationBtn.setOnClickListener{getCurrentLocation()}
        clrBtn.setOnClickListener { defaultFields() }
        dateEdit.setOnClickListener{setDate()}
        sPref = PreferenceManager.getDefaultSharedPreferences(this)
        //calcBtn.setOnClickListener{foo()}
        defaultFields()
        //Toast.makeText(this, sPref.getString("model", "0"), Toast.LENGTH_LONG).show()
        //supportActionBar?.setDisplayShowTitleEnabled(false)
    }



    override fun onResume() {
        super.onResume()
        if(mGpsUtils.checkLocationPermissions(this))
                mGpsUtils.startLocationUpdates(this)
    }

    override fun onPause() {
        super.onPause()
        mGpsUtils.stoplocationUpdates()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.settingsBut -> {

                openSettings()
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode == MY_PERMISSIONS_REQUEST_LOCATION){
            if (grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, this)
                }
            }
        }
    }

    protected fun startLocationUpdates() {

        // Create the location request to start receiving updates

        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest!!.setInterval(INTERVAL)
        mLocationRequest!!.setFastestInterval(FASTEST_INTERVAL)

        // Create LocationSettingsRequest object using location request
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        val locationSettingsRequest = builder.build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(locationSettingsRequest)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


            return
        }
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper())
        isLocationGoing = true
    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // do work here
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location) {
        // New location has now been determined

        mLastLocation = location
        isLocationChanged = !isLocationChanged
        // You can now create a LatLng Object for use with maps
    }

    private fun stoplocationUpdates() {
        if(mFusedLocationProviderClient != null)
            mFusedLocationProviderClient!!.removeLocationUpdates(mLocationCallback)
        isLocationGoing = false
    }



    private fun checkLocationPermissions():Boolean{
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this,
                                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                                MY_PERMISSIONS_REQUEST_LOCATION)
            return false
        }
        return true
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(){
            if(!checkLocationPermissions()) return
            if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                buildAlertMessageNoGps()
            }
        else{
                if(!isLocationGoing) startLocationUpdates()
                GlobalScope.launch(Dispatchers.Main) {
                    progressLayout?.visibility = View.VISIBLE
                    val state = isLocationChanged
                    var timeout = 10
                    while (state == isLocationChanged){
                        delay(2000)
                        timeout--
                        if (timeout<=0){
                            Toast.makeText(this@MainActivity, getString(R.string.bad_signal), Toast.LENGTH_LONG).show()
                            progressLayout?.visibility = View.GONE
                            return@launch
                        }

                    }
                    progressLayout?.visibility = View.GONE
                    latitudeEdit.setText("%.3f".format(mLastLocation.latitude).replace(',','.'))
                    longitudeEdit.setText("%.3f".format(mLastLocation.longitude).replace(',','.'))
                    altitudeEdit.setText(mLastLocation.altitude.toString())
                }
            }



    }

    private fun getCurrentLocation(){
        if(mGpsUtils.checkLocationPermissions(this)){
            if(mGpsUtils.checkLocationProvider(this)){
                mGpsUtils.startLocationUpdates(this)
                GlobalScope.launch(Dispatchers.Main) {
                    progressLayout?.visibility = View.VISIBLE
                    var location = mGpsUtils.mLastLocation
                    var timeout = 10
                    while ((timeout--)>0){
                        delay(2000)
                        if(mGpsUtils.mLastLocation != location){
                            progressLayout?.visibility = View.GONE
                            latitudeEdit.setText("%.3f".format(mGpsUtils.mLastLocation?.latitude?:0).replace(',','.'))
                            longitudeEdit.setText("%.3f".format(mGpsUtils.mLastLocation?.longitude?:0).replace(',','.'))
                            altitudeEdit.setText((mGpsUtils.mLastLocation?.altitude?:0).toString())
                            return@launch
                        }

                    }
                    Toast.makeText(this@MainActivity, getString(R.string.bad_signal), Toast.LENGTH_LONG).show()
                    progressLayout?.visibility = View.GONE
                }
            }
        }

    }

    private fun buildAlertMessageNoGps() {

        val builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.gps_seems_turnoff))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.yes)) { dialog, id ->
                startActivityForResult(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    , 11)
            }
            .setNegativeButton(getString(R.string.no)) { dialog, id ->
                dialog.cancel()

            }
        val alert: AlertDialog  = builder.create()
        alert.show()


    }


    private fun openSettings(){
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    private fun setDate(){
        val dFormatter = DateFormatter(DateFormat.DMY)
        //dateEdit.setText(dFormatter.getCurrentDate())
        dFormatter.changeDate(this, DatePickerDialog.OnDateSetListener(::dateListener))
    }


    private fun dateListener(dPD: DatePicker, year: Int, month: Int, day: Int){

            val result = DateFormatter(DateFormat.DMY).getStringFromDate(UserDate(day, month+1, year))
            dateEdit.setText(result)
    }

    private fun defaultFields(){
        latitudeEdit.setText("")
        longitudeEdit.setText("")
        altitudeEdit.setText("")
        bHorRes.setText("")
        refBtres.setText("")
        DIPres.setText("")
        INCLres.setText("")
        dateEdit.setText(DateFormatter(DateFormat.DMY).getCurrentDate())
    }
}
