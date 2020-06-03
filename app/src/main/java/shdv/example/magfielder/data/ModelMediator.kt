package shdv.example.magfielder.data

import android.content.SharedPreferences
import shdv.example.magfielder.Utils.DateFormat
import shdv.example.magfielder.Utils.DateFormatter

class ModelMediator(sPref: SharedPreferences) {
    val model:ModelDispatcher
    private val modelType = sPref.getString("model", "0")?.toInt()?:0
    private val shape = sPref.getString("shape", "0")?.toInt()?:0
    private val dateFormat = sPref.getString("dformat", "dd/MM/yyyy")

    private var resulListeners: ArrayList<(FieldResult) -> Unit> = arrayListOf(fun(_:FieldResult){})
    private var errorListeners: ArrayList<(String)->Unit> = arrayListOf(fun(_:String){})

    init {

        model = when(modelType){
            0 -> IGRFDispather()
            1 -> WMMDispatcher()
            else -> IGRFDispather()
        }
    }

    fun apply(lat: Double, lon: Double, alt: Double, _date: String){
        try{
           if (!model.validateIn(lat, lon, alt)) {
               errorHappened("Invalid data.")
               return
           }
            val date = model.dateFormat(_date, dateFormat as DateFormat)
            val coeffs = model.getCoeffsByDate(date)
            if(coeffs == null) {
                errorHappened("An error occurred.")
                return
            }
            val  gParams = model.prepare(lat, alt, shape)
            val result = model.getResult(gParams, lon, coeffs)
            resultUpdated(result)
        }
        catch (e:ModelException){
            errorHappened(e.toString())
        }
        catch (e: Exception){
            errorHappened("An error occurred.")
        }
    }

    fun setResultListener(del:(FieldResult) -> Unit){
        resulListeners.add(del)
    }

    fun removeResultListener(del: (FieldResult) -> Unit){
        resulListeners.remove(del)
    }

    fun setErrorListener(del: (String) -> Unit){
        errorListeners.add(del)
    }

    fun removeErrorListener(del: (String) -> Unit){
        errorListeners.remove(del)
    }

    private fun resultUpdated(result: FieldResult){
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