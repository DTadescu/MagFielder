package shdv.example.magfielder

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.nfc.FormatException
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import shdv.example.magfielder.data.FieldResult
import shdv.example.magfielder.data.ModelMediator
import shdv.example.magfielder.databinding.ActivityMainBinding
import shdv.example.magfielder.databinding.InfoDialogBinding
import shdv.example.magfielder.utils.DateFormat
import shdv.example.magfielder.utils.DateFormatter
import shdv.example.magfielder.utils.GpsUtils
import shdv.example.magfielder.utils.ReportFormat
import shdv.example.magfielder.utils.ReportUtil
import shdv.example.magfielder.utils.UIHelper
import shdv.example.magfielder.utils.UserDate


class MainActivity : AppCompatActivity() {
    private lateinit var mGpsUtils: GpsUtils
    private lateinit var dateFormat: DateFormat
    private lateinit var sPref: SharedPreferences
    private lateinit var binding: ActivityMainBinding
    private var modeler: ModelMediator? = null
    private var latSign = 1
    private var longSign = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContentView(binding.root)

        mGpsUtils = GpsUtils(getSystemService(LOCATION_SERVICE) as LocationManager)
        binding.setlocationBtn.setOnClickListener { getCurrentLocation() }
        binding.clrBtn.setOnClickListener { defaultFields() }
        binding.dateEdit.setOnClickListener { setDate() }
        binding.btnShare.setOnClickListener { shareResult() }
        binding.btnCopy.setOnClickListener { copyResult() }
        sPref = PreferenceManager.getDefaultSharedPreferences(this)
        setVisiblity()
        binding.calcBtn.setOnClickListener { calcModel() }
        binding.btnLat.setOnClickListener { actionLatBtn() }
        binding.btnLong.setOnClickListener { actionLongBtn() }
        defaultFields()
    }


    override fun onResume() {
        super.onResume()
        if (mGpsUtils.checkLocationPermissions(this))
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
        when (item.itemId) {
            R.id.settingsBut -> {

                openSettings()
            }

            R.id.infoBut -> {
                openHelp()
            }
        }
        return true
    }

    private fun openHelp() {
        val binding = InfoDialogBinding.inflate(layoutInflater)

        val dialog = UIHelper.getSimpleDialog(this, binding.root)

        binding.okInfo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun OnResultListener(result: FieldResult) {
        binding.refBtres.text = "%.1f".format(result.Btot).replace(',', '.')
        binding.DeclRes.text = "%.2f".format(result.Dec).replace(',', '.')
        binding.INCLres.text = "%.2f".format(result.Inc).replace(',', '.')
        binding.bHorRes.text = "%.1f".format(result.Bhor).replace(',', '.')
        binding.nCompRes.text = "%.1f".format(result.North).replace(',', '.')
        binding.eCompRes.text = "%.1f".format(result.East).replace(',', '.')
        binding.vCompRes.text = "%.1f".format(result.Vert).replace(',', '.')

        binding.progressLayout.visibility = View.GONE
    }

    private fun OnErrorOccurred(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        binding.progressLayout.visibility = View.GONE
    }

    private fun calcModel() {
        if (isEmpty(binding.latitudeEdit)) return
        if (isEmpty(binding.longitudeEdit)) return
        if (isEmpty(binding.altitudeEdit)) return
        if (modeler != null) {
            modeler!!.removeErrorListener(::OnErrorOccurred)
            modeler!!.removeResultListener(::OnResultListener)
        }
        modeler = ModelMediator(sPref)
        modeler!!.setResultListener(::OnResultListener)
        modeler!!.setErrorListener(::OnErrorOccurred)
        Log.d("NEWMODEL", "created model")

        try {
            latSign = when {
                binding.btnLat.text == getString(R.string.northLatButton) -> 1
                binding.btnLat.text == getString(R.string.southLatButton) -> -1
                else -> 1
            }
            longSign = when {
                binding.btnLong.text == getString(R.string.eastLongButton) -> 1
                binding.btnLong.text == getString(R.string.westLongButton) -> -1
                else -> 1
            }
            binding.progressLayout.visibility = View.VISIBLE
            //GlobalScope.launch(Dispatchers.Main) {
            modeler!!.doWork(
                binding.latitudeEdit.text.toString().toDouble() * latSign,
                binding.longitudeEdit.text.toString().toDouble() * longSign,
                binding.altitudeEdit.text.toString().toDouble(),
                binding.dateEdit.text.toString()
            )

            //}

        } catch (e: FormatException) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.format_exception), Toast.LENGTH_LONG).show()
        }
    }

    private fun isEmpty(view: EditText): Boolean {
        if (view.text.isEmpty()) {
            Toast.makeText(this, getString(R.string.field_is_empty), Toast.LENGTH_LONG).show()
            return true
        }
        return false
    }

    private fun getCurrentLocation() {
        if (mGpsUtils.checkLocationPermissions(this)) {
            if (mGpsUtils.checkLocationProvider(this)) {
                mGpsUtils.startLocationUpdates(this)
                GlobalScope.launch(Dispatchers.Main) {
                    binding.progressLayout.visibility = View.VISIBLE
                    val location = mGpsUtils.mLastLocation
                    var timeout = 10
                    while ((timeout--) > 0) {
                        delay(2000)
                        if (mGpsUtils.mLastLocation != location) {
                            binding.progressLayout.visibility = View.GONE
                            binding.latitudeEdit.setText(
                                "%.4f".format(
                                    mGpsUtils.mLastLocation?.latitude ?: 0
                                ).replace(',', '.').let {
                                    if (it.startsWith("-")) {
                                        binding.btnLat.text = getString(R.string.southLatButton)
                                        it.removePrefix("-")
                                    } else {
                                        binding.btnLat.text = getString(R.string.northLatButton)
                                        it
                                    }
                                }
                            )
                            binding.longitudeEdit.setText(
                                "%.4f".format(
                                    mGpsUtils.mLastLocation?.longitude ?: 0
                                ).replace(',', '.').let {
                                    if (it.startsWith("-")) {
                                        binding.btnLong.text = getString(R.string.westLongButton)
                                        it.removePrefix("-")
                                    } else {
                                        binding.btnLong.text = getString(R.string.eastLongButton)
                                        it
                                    }
                                }
                            )
                            binding.altitudeEdit.setText(
                                (mGpsUtils.mLastLocation?.altitude ?: 0).toString()
                            )
                            return@launch
                        }

                    }
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.bad_signal),
                        Toast.LENGTH_LONG
                    ).show()
                    binding.progressLayout.visibility = View.GONE
                }
            }
        }

    }

    private fun copyResult() {
        val result = zipResult(ReportFormat.TXT)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", result)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copy_message), Toast.LENGTH_LONG).show()
    }

    private fun shareResult() {
        val result = zipResult(ReportFormat.TXT)
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, result)
        sendIntent.type = "text/plain"
        startActivity(Intent.createChooser(sendIntent, getString(R.string.send_to)))
    }

    private fun zipResult(format: ReportFormat): String {
        val reporter =
            ReportUtil(format, getString(R.string.date), binding.dateEdit.text.toString())
        reporter.add(getString(R.string.latitude), binding.latitudeEdit.text.toString())
        reporter.add(getString(R.string.longitude), binding.longitudeEdit.text.toString())
        reporter.add(getString(R.string.altitude), binding.altitudeEdit.text.toString())
        if (binding.btotLayout.isVisible)
            reporter.add(getString(R.string.Btotal), binding.refBtres.text.toString())
        if (binding.declLayout.isVisible)
            reporter.add(getString(R.string.Decl), binding.DeclRes.text.toString())
        if (binding.inclLayout.isVisible)
            reporter.add(getString(R.string.INCL), binding.INCLres.text.toString())
        if (binding.bxyLayout.isVisible)
            reporter.add(getString(R.string.Bhor), binding.bHorRes.text.toString())
        if (binding.ncompLayout.isVisible)
            reporter.add(getString(R.string.north_comp), binding.nCompRes.text.toString())
        if (binding.ecompLayout.isVisible)
            reporter.add(getString(R.string.east_comp), binding.eCompRes.text.toString())
        if (binding.vcompLayout.isVisible)
            reporter.add(getString(R.string.vert_comp), binding.vCompRes.text.toString())

        return reporter.report
    }

    private fun openSettings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    private fun setDate() {
        val dFormatter = DateFormatter(DateFormat.DMY)
        dFormatter.changeDate(this, DatePickerDialog.OnDateSetListener(::dateListener))
    }


    private fun dateListener(dPD: DatePicker, year: Int, month: Int, day: Int) {

        val result = DateFormatter(
            DateFormat.getFormat(
                sPref.getString("dateformat", "dd/MM/yyyy") ?: "yyyy/MM/dd"
            )
        )
            .getStringFromDate(UserDate(day, month + 1, year))
        binding.dateEdit.text = result
    }

    private fun defaultFields() {
        binding.latitudeEdit.setText("")
        binding.longitudeEdit.setText("")
        binding.altitudeEdit.setText("")
        binding.bHorRes.text = ""
        binding.refBtres.text = ""
        binding.DeclRes.text = ""
        binding.INCLres.text = ""
        binding.nCompRes.text = ""
        binding.eCompRes.text = ""
        binding.vCompRes.text = ""
        dateFormat =
            DateFormat.getFormat(sPref.getString("dateformat", "dd/MM/yyyy") ?: "yyyy/MM/dd")
        binding.dateEdit.text = DateFormatter(dateFormat)
            .getCurrentDate()
    }

    private fun checkDate() {
        val newFormat =
            DateFormat.getFormat(sPref.getString("dateformat", "dd/MM/yyyy") ?: "yyyy/MM/dd")
        if (dateFormat != newFormat) {
            val date = DateFormatter(dateFormat).getDateFromString(binding.dateEdit.text.toString())
            binding.dateEdit.text = DateFormatter(newFormat).getStringFromDate(date)
            dateFormat = newFormat
        }
    }

    private fun setVisiblity() {
        binding.btotLayout.visibility =
            if (sPref.getBoolean("btotal", true)) View.VISIBLE else View.GONE
        binding.declLayout.visibility =
            if (sPref.getBoolean("declination", true)) View.VISIBLE else View.GONE
        binding.inclLayout.visibility =
            if (sPref.getBoolean("inclination", true)) View.VISIBLE else View.GONE
        binding.bxyLayout.visibility =
            if (sPref.getBoolean("bxy", false)) View.VISIBLE else View.GONE
        binding.ncompLayout.visibility =
            if (sPref.getBoolean("northComp", false)) View.VISIBLE else View.GONE
        binding.ecompLayout.visibility =
            if (sPref.getBoolean("eastComp", false)) View.VISIBLE else View.GONE
        binding.vcompLayout.visibility =
            if (sPref.getBoolean("vertComp", false)) View.VISIBLE else View.GONE
    }

    private fun actionLatBtn() {
        val value = when {
            binding.btnLat.text == getString(R.string.northLatButton) -> getString(R.string.southLatButton)
            binding.btnLat.text == getString(R.string.southLatButton) -> getString(R.string.northLatButton)
            else -> getString(R.string.northLatButton)
        }
        binding.btnLat.text = value
    }

    private fun actionLongBtn() {
        val value = when {
            binding.btnLong.text == getString(R.string.eastLongButton) -> getString(R.string.westLongButton)
            binding.btnLong.text == getString(R.string.westLongButton) -> getString(R.string.eastLongButton)
            else -> getString(R.string.eastLongButton)
        }
        binding.btnLong.text = value
    }
}
