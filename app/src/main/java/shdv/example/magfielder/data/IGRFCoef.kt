package shdv.example.magfielder.data

import kotlin.math.pow

object IGRFCoef {
    //var delegate:((Double, Double, Double, Double)->DoubleArray) = ::interp1d
    private val startDate = 2020.0
    private val endDate = 2025.0
    private val startArray = doubleArrayOf(-29404.8, -1450.9, 4652.5, -2499.6, 2982.0, -2991.6,
        1677.0, -734.6, 1363.2, -2381.2, -82.1, 1236.2, 241.9, 525.7, -543.4, 903.0, 809.5, 281.9,
        86.3, -158.4, -309.4, 199.7, 48.0, -349.7, -234.3, 363.2, 47.7, 187.8, 208.3, -140.7,
        -121.2, -151.2, 32.3, 13.5, 98.9, 66.0, 65.5, -19.1, 72.9, 25.1, -121.5, 52.8, -36.2, -64.5,
        13.5, 8.9, -64.7, 68.1, 80.6, -76.7, -51.5, -8.2, -16.9, 56.5, 2.2, 15.8, 23.5, 6.4, -2.2,
        -7.2, -27.2, 9.8, -1.8, 23.7, 9.7, 8.4, -17.6, -15.3, -0.5, 12.8, -21.1, -11.7, 15.3, 14.9,
        13.7, 3.6, -16.5, -6.9, -0.3)
    private val endArray = doubleArrayOf(-29376.3, -1413.9, 4523.0, -2554.6, 2947.0, -3142.6,
        1666.5, -846.6, 1374.2, -2410.7, -52.1, 1251.7, 236.4, 465.7, -540.9, 897.0, 801.5, 281.4,
        56.8, -125.9, -283.4, 217.7, 22.5, -374.7, -235.8, 365.7, 47.7, 184.8, 220.8, -139.7,
        -124.2, -144.7, 47.3, 18.0, 100.4, 63.5, 64.0, -19.1, 74.9, 17.1, -115.0, 46.3, -43.2,
        -60.5, 13.5, 8.9, -60.2, 73.1, 80.1, -77.7, -48.5, -8.2, -13.9, 60.0, -1.8, 16.3, 22.5, 3.9,
        -7.7, -11.2, -26.7, 13.8, -0.3, 23.7, 10.2, 7.4, -18.1, -12.3, 1.5, 11.8, -21.6, -9.2, 17.3,
        13.4, 15.2, 1.6, -17.0, -4.4, 1.7)
    val coefArray = getFuncArray()

    private fun getFuncArray():ArrayList<DoubleArray>?{
        val count = if(startArray.size < endArray.size)
                            startArray.size
                    else endArray.size
        if (count > 0){
            val coefArray:ArrayList<DoubleArray> = arrayListOf()
            for (f in 0 until count){
                coefArray.add(interp1d(startArray[f], endArray[f], startDate, endDate))
            }
            return coefArray
        }
        return null
    }

    private fun interp1d(x0: Double, x1: Double, y0: Double, y1: Double):DoubleArray{
        if (x0 == x1) return doubleArrayOf(0.0, 0.0)
        val k = (y1-y0)/(x1-x0)
        val b = y1 - k*x1
        return doubleArrayOf(b, k)
    }

    fun calculateCoeffs(point: Double): ArrayList<Double>?{
        if (coefArray == null) return null
        if (coefArray.size < 1) return null
        val resArray = ArrayList<Double>(coefArray.size)
        for (pol in coefArray){
            if(pol.size < 2) return null
            var coef = 0.0
            for(mul in pol.indices){
                coef += pol[mul]*point.pow(mul)
            }
            resArray.add(coef)
        }
        return resArray
    }
}