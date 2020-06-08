package shdv.example.magfielder.data

import android.content.SharedPreferences
import android.util.Log
import shdv.example.magfielder.Utils.DateFormat
import shdv.example.magfielder.Utils.DateFormatter

class ModelMediator(sPref: SharedPreferences) {
    val model:ModelDispatcher
    private val modelType =
        try{
            sPref.getString("model", "0")!!.toInt()
        }
        catch(e:Exception){
            e.printStackTrace()
            0
        }
    private val shape = try{
        sPref.getString("shape", "1")!!.toInt()
    }
    catch (e:Exception){
        e.printStackTrace()
        1
    }
    private val dateFormat = sPref.getString("dateformat","dd/MM/yyyy")?:"yyyy/MM/dd"

    private var resulListeners: ArrayList<(FieldResult) -> Unit> = arrayListOf(fun(_:FieldResult){})
    private var errorListeners: ArrayList<(String)->Unit> = arrayListOf(fun(_:String){})

    init {

         when(modelType){
            0 -> {
                model = IGRFDispather(shape)
                Log.d("NEWMODEL","IGRF")
            }
            else -> {
                model = IGRFDispather(shape)
                Log.d("NEWMODEL","Strange")
            }
        }
    }

    fun doWork(lat: Double, lon: Double, alt: Double, _date: String){
        try{
            Log.d("CALC", "Start calc")
           if (!model.validateIn(lat, lon, alt)) {
               errorHappened("Invalid data.")
               return
           }
            val date = model.dateFormat(_date, DateFormat.getFormat(dateFormat))
            Log.d("CALC", "Date is done ${date}")
            val coeffs = model.getCoeffsByDate(date)
            Log.d("CALC", "Coeffs is done")
            if(coeffs == null) {
                errorHappened("An error occurred.")
                return
            }
            val  gParams = model.prepare(lat, alt)
            Log.d("CALC", "Geoparams is done ${gParams.radius} ${gParams.theta} ${gParams.sd} ${gParams.cd}")
            val result = model.getResult(gParams, lon, coeffs)
            Log.d("CALC", "done result")
            resultUpdated(result)
        }
        catch (e:ModelException){
            e.printStackTrace()
            errorHappened(e.toString())
        }
        catch (e: Exception){
            e.printStackTrace()
            errorHappened("An error occurred.")
        }
    }

    fun setResultListener(del:(FieldResult) -> Unit){
        resulListeners.add(del)
    }

    fun removeResultListener(del: (FieldResult) -> Unit){
        try {
            resulListeners.remove(del)
        }
        catch(e: Exception) {e.printStackTrace()}
    }

    fun setErrorListener(del: (String) -> Unit){
        errorListeners.add(del)
    }

    fun removeErrorListener(del: (String) -> Unit){
        try {
            errorListeners.remove(del)
        }
        catch(e: Exception) {e.printStackTrace()}
    }

    private fun resultUpdated(result: FieldResult){
        Log.d("RESULT", "${result.Btot}")
        for (del in resulListeners){
            del(result)
        }
    }

    private fun errorHappened(errMes: String){
        for(del in errorListeners){
            del(errMes)
        }
    }
}