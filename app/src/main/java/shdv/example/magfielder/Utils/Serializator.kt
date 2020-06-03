package shdv.example.magfielder.Utils

import android.content.Context
import android.util.Log
import java.io.Serializable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import com.google.gson.Gson
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.util.List
import kotlin.reflect.typeOf

class JSONSerializator {

    public fun serializeAsync(context: Context, obj:Serializable, path:String)=
        GlobalScope.async {
            val gson = Gson()
            val jsonString = gson.toJson(obj)
            var outputStream:FileOutputStream? = null
            try {
                outputStream = context.openFileOutput(path, Context.MODE_PRIVATE)
                outputStream.write(jsonString.toByteArray())
                return@async true
            }
            catch (e:Exception){
                e.printStackTrace()
                Log.d("SERERROR",e.toString())
            }
            finally {
                if (outputStream != null){
                    try {
                        outputStream.close()
                    }
                    catch (e:IOException){
                        e.printStackTrace()
                    }
                }
            }
            return@async false
        }


    public fun deserializeAsync(context: Context, path: String, obj: Any) =
        GlobalScope.async {
            var streamReader:InputStreamReader? = null
            var inputStream:FileInputStream? = null
            try {
                inputStream = context.openFileInput(path)
                streamReader = InputStreamReader(inputStream)
                val gson = Gson()
                val test:SettingsUtil = gson.fromJson(streamReader, obj.javaClass) as SettingsUtil
                Log.d("DESERJ", "${test.langS.value}")
                return@async test
            }
            catch (e: Exception){
                e.printStackTrace()
                Log.d("DESERERROR",e.toString())
            }
            finally {
                if (streamReader != null){
                    try {
                        streamReader.close()
                    }
                    catch (e:IOException){
                        e.printStackTrace()
                    }
                }
                if(inputStream != null){
                    try {
                        inputStream.close()
                    }
                    catch (e:IOException){
                        e.printStackTrace()
                    }
                }
            }
            return@async obj
        }

}