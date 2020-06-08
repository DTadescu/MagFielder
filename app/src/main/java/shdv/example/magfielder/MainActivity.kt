package shdv.example.magfielder

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.*
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.nfc.FormatException
import com.google.android.gms.location.*
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
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
import shdv.example.magfielder.data.FieldResult
import shdv.example.magfielder.data.ModelMediator
import java.time.Year
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var mGpsUtils:GpsUtils

    private lateinit var dateFormat: DateFormat
    private lateinit var sPref: SharedPreferences
    private var modeler:ModelMediator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        //if(!isInit) initSettings()
        //runBlocking { delay(500) }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
       mGpsUtils = GpsUtils(getSystemService(LOCATION_SERVICE) as LocationManager)
        setlocationBtn.setOnClickListener{getCurrentLocation()}
        clrBtn.setOnClickListener { defaultFields() }
        dateEdit.setOnClickListener{setDate()}
        btnShare.setOnClickListener{shareResult()}
        btnCopy.setOnClickListener{copyResult()}
        sPref = PreferenceManager.getDefaultSharedPreferences(this)
        setVisiblity()
        calcBtn.setOnClickListener{calcModel()}
        defaultFields()
        //Toast.makeText(this, sPref.getString("model", "0"), Toast.LENGTH_LONG).show()
        //supportActionBar?.setDisplayShowTitleEnabled(false)
    }



    override fun onResume() {
        super.onResume()
        if(mGpsUtils.checkLocationPermissions(this))
                mGpsUtils.startLocationUpdates(this)
        setVisiblity()
        checkDate()
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

    private fun OnResultListener(result: FieldResult){
        refBtres.setText("%.1f".format(result.Btot).replace(',','.'))
        DeclRes.setText("%.2f".format(result.Dec).replace(',','.'))
        INCLres.setText("%.2f".format(result.Inc).replace(',','.'))
        bHorRes.setText("%.1f".format(result.Bhor).replace(',','.'))
        nCompRes.setText("%.1f".format(result.North).replace(',','.'))
        eCompRes.setText("%.1f".format(result.East).replace(',','.'))
        vCompRes.setText("%.1f".format(result.Vert).replace(',','.'))

        progressLayout?.visibility = View.GONE
    }

    private fun OnErrorOccurred(message: String){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        progressLayout?.visibility = View.GONE
    }

    private fun calcModel(){
        if(isEmpty(latitudeEdit)) return
        if(isEmpty(longitudeEdit)) return
        if(isEmpty(altitudeEdit)) return
        if(modeler != null){
            modeler!!.removeErrorListener(::OnErrorOccurred)
            modeler!!.removeResultListener(::OnResultListener)
        }
            modeler = ModelMediator(sPref)
            modeler!!.setResultListener(::OnResultListener)
            modeler!!.setErrorListener(::OnErrorOccurred)
            Log.d("NEWMODEL", "created model")

        try {
            progressLayout?.visibility = View.VISIBLE
            //GlobalScope.launch(Dispatchers.Main) {
                modeler!!.doWork(latitudeEdit.text.toString().toDouble(),
                    longitudeEdit.text.toString().toDouble(),
                    altitudeEdit.text.toString().toDouble(),
                    dateEdit.text.toString())

            //}

        }
        catch (e: FormatException){
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.format_exception), Toast.LENGTH_LONG).show()
        }
    }

    private fun isEmpty(view: EditText):Boolean{
        if(view.text.isEmpty()){
            Toast.makeText(this, getString(R.string.field_is_empty), Toast.LENGTH_LONG).show()
            return true
        }
            return false
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
                            latitudeEdit.setText("%.4f".format(mGpsUtils.mLastLocation?.latitude?:0).replace(',','.'))
                            longitudeEdit.setText("%.4f".format(mGpsUtils.mLastLocation?.longitude?:0).replace(',','.'))
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

    private fun copyResult(){
        val result = zipResult(ReportFormat.TXT)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", result)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copy_message), Toast.LENGTH_LONG).show()
    }

    private fun shareResult(){
        val result = zipResult(ReportFormat.TXT)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, result)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, getString(R.string.send_to)))
    }

    private fun zipResult(format: ReportFormat):String{
        val reporter = ReportUtil(format, getString(R.string.date), dateEdit.text.toString())
        reporter.add(getString(R.string.latitude), latitudeEdit.text.toString())
        reporter.add(getString(R.string.longitude), longitudeEdit.text.toString())
        reporter.add(getString(R.string.altitude), altitudeEdit.text.toString())
        if(btot_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.Btotal), refBtres.text.toString())
        if(decl_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.Decl), DeclRes.text.toString())
        if(incl_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.INCL), INCLres.text.toString())
        if(bxy_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.Bhor), bHorRes.text.toString())
        if(ncomp_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.north_comp), nCompRes.text.toString())
        if(ecomp_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.east_comp), eCompRes.text.toString())
        if(vcomp_layout.visibility == View.VISIBLE)
            reporter.add(getString(R.string.vert_comp), vCompRes.text.toString())

        return reporter.report
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

            val result = DateFormatter(DateFormat.getFormat(sPref.getString("dateformat","dd/MM/yyyy")?:"yyyy/MM/dd"))
                .getStringFromDate(UserDate(day, month+1, year))
            dateEdit.setText(result)
    }

    private fun defaultFields(){
        latitudeEdit.setText("")
        longitudeEdit.setText("")
        altitudeEdit.setText("")
        bHorRes.setText("")
        refBtres.setText("")
        DeclRes.setText("")
        INCLres.setText("")
        nCompRes.setText("")
        eCompRes.setText("")
        vCompRes.setText("")
        dateFormat = DateFormat.getFormat(sPref.getString("dateformat","dd/MM/yyyy")?:"yyyy/MM/dd")
        dateEdit.setText(DateFormatter(dateFormat)
            .getCurrentDate())
    }

    private fun checkDate(){
        val newFormat = DateFormat.getFormat(sPref.getString("dateformat","dd/MM/yyyy")?:"yyyy/MM/dd")
        if(dateFormat != newFormat){
            val date = DateFormatter(dateFormat).getDateFromString(dateEdit.text.toString())
            dateEdit.setText(DateFormatter(newFormat).getStringFromDate(date))
            dateFormat = newFormat
        }
    }

    private fun setVisiblity(){
        btot_layout.visibility = if(sPref.getBoolean("btotal", true)) View.VISIBLE else View.GONE
        decl_layout.visibility = if(sPref.getBoolean("declination", true)) View.VISIBLE else View.GONE
        incl_layout.visibility = if(sPref.getBoolean("inclination", true)) View.VISIBLE else View.GONE
        bxy_layout.visibility = if(sPref.getBoolean("bxy", false)) View.VISIBLE else View.GONE
        ncomp_layout.visibility = if(sPref.getBoolean("northComp", false)) View.VISIBLE else View.GONE
        ecomp_layout.visibility = if(sPref.getBoolean("eastComp", false)) View.VISIBLE else View.GONE
        vcomp_layout.visibility = if(sPref.getBoolean("vertComp", false)) View.VISIBLE else View.GONE
    }
}
