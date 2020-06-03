package shdv.example.magfielder.data

import shdv.example.magfielder.Utils.DateFormat
import shdv.example.magfielder.Utils.DateFormatter
import java.lang.Exception

open class ModelDispatcher {

    fun validateIn(latd: Double, lond: Double, alt: Double):Boolean{
        if(alt < -2800000) return false
        return IOValidator.checkLatLonBounds(latd, lond)
    }

    fun dateFormat(_date: String, _format: DateFormat):Double{
        val formatter = DateFormatter(_format)
        return formatter.getDecimalDate(_date)
    }

    fun prepare(lat: Double, alt: Double, shape: Int):GeoParameters{
        val colat = 90 - lat
        return  when(shape){
                0 -> GeoParameters(6371.3 + alt/1000, colat, 0.0, 0.0)
                1 -> IgrfUtils.ggToGeo(alt, colat)
                else -> IgrfUtils.ggToGeo(alt, colat)
            }
    }

    fun getCoeffsByDate(date: Double):ArrayList<Double>?{
        return IGRFCoef.calculateCoeffs(date)
    }


    fun getResult(gParams:GeoParameters, lon: Double, coeffs: ArrayList<Double>):FieldResult{
        val components = IgrfUtils.synthValues(coeffs, gParams.radius, gParams.theta, lon)
        val x = -components.bTheta
        val y = components.bPhi
        val z = -components.bRadius
        return IgrfUtils.xyz2dhif(x, y , z)
    }

}