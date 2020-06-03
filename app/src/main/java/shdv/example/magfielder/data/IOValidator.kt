package shdv.example.magfielder.data

object IOValidator {

    fun checkLatLonBounds(latd: Double, lond: Double):Boolean{
        if(latd >= 90.0 || latd <= -90.0)
            return false
        if(lond >= 360.0 || lond <= -360.0)
            return false
        return true
    }

    fun checkDouble(value: String):Double{
        try {
            return value.toDouble()
        }
        catch (e: Exception){
            throw ModelException("Illegal data format.")
        }
    }
}