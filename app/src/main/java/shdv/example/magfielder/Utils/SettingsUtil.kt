package shdv.example.magfielder.Utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import shdv.example.magfielder.MainActivity
import shdv.example.magfielder.R
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList
//import kotlinx.serialization.Serializable


class SettingsUtil: Serializable {

companion object{
    private const val path = "settings.dat"
    private var listeners:ArrayList<(lang:String) -> Unit> = arrayListOf(fun(_:String){})
    private var onErrorListeners:ArrayList<(lang:String) -> Unit> = arrayListOf(fun(_:String){})

    var lang:Language = Language.ENGLISH
        private set(value){
            field = value
        }
    var model:Model = Model.IGRF
        private set(value){
            field = value
        }
    var shape:EarthShape = EarthShape.SPHERE
        private set(value){
            field = value
        }

    private fun settingChanged(){
        for (del in listeners){
            del.invoke(lang.value)
        }
    }

    private fun onError(errorMes: String){
        for (del in onErrorListeners){
            del.invoke(errorMes)
        }
    }

    fun addListener(delegate: (lang:String) -> Unit){
        listeners.add { delegate }
    }

    fun removeListener(delegate: (lang:String) -> Unit){
        listeners.remove { delegate }
    }

    fun addErrorListener(delegate: (lang:String) -> Unit){
        onErrorListeners.add { delegate }
    }

    fun removeErrorListener(delegate: (lang:String) -> Unit){
        onErrorListeners.remove { delegate }
    }

    fun setValues(language: Language, model: Model, shape: EarthShape){
        this.lang = language
        this.model = model
        this.shape = shape
        settingChanged()
    }


    suspend fun serialize(context: Context){
        val serial = JSONSerializator()
        var obj = SettingsUtil(lang, model, shape)
        if(!(serial.serializeAsync(context, obj, path).await())){
            onError(context.getString(R.string.serialError))
        }
    }

    suspend fun deserialize(context: Context){
        val serial = JSONSerializator()
        var obj = SettingsUtil(lang, model, shape)
        obj = serial.deserializeAsync(context, path, obj).await() as SettingsUtil
        Log.d("DESER","${obj.langS.value} ${obj.modelS.value} ${obj.shapeS.value}")
        if(obj != null){
            lang = obj.langS
            model = obj.modelS
            shape = obj.shapeS
        }
}


    }
    constructor(language: Language, model: Model, shape: EarthShape){
        langS = language
        modelS = model
        shapeS = shape
    }

    var langS:Language = Language.ENGLISH

    private var modelS:Model = Model.IGRF

    private var shapeS:EarthShape = EarthShape.SPHERE


}



