package shdv.example.magfielder.Utils

import kotlin.math.cos
import kotlin.math.sin

object Operation {
    fun arrayMulDouble(array: DoubleArray, arg: Double):DoubleArray{
        val newarr = array.clone()
        for(n in array.indices){
            newarr[n] = array[n]*arg
        }
        return  newarr
    }

    fun cosArray(array: DoubleArray):DoubleArray{
        val newarr = array.clone()
        for(n in array.indices){
            newarr[n] = cos(array[n])
        }
        return  newarr
    }

    fun sinArray(array: DoubleArray):DoubleArray{
        val newarr = array.clone()
        for(n in array.indices){
            newarr[n] = sin(array[n])
        }
        return  newarr
    }
}


