package shdv.example.magfielder.data

import java.io.InputStream
import kotlin.math.pow

object IGRFCoef {
    private var isInit = false
    private var yearsMap = YearsMap()

    private fun getFuncArray(): ArrayList<DoubleArray>? {
        val startArray = yearsMap.startArray
            ?: throw IllegalArgumentException("IGRF coefficients not yet initialized")
        val endArray = yearsMap.endArray
            ?: throw IllegalArgumentException("IGRF coefficients not yet initialized")
        val startDate = yearsMap.startDate
            ?: throw IllegalArgumentException("IGRF coefficients not yet initialized")
        val endDate = yearsMap.endDate
            ?: throw IllegalArgumentException("IGRF coefficients not yet initialized")
        val count = if (startArray.size < endArray.size)
            startArray.size
        else endArray.size
        if (count > 0) {
            val coefArray: ArrayList<DoubleArray> = arrayListOf()
            for (f in 0 until count) {
                //coefArray.add(interp1d(startArray[f], endArray[f], startDate, endDate))
                coefArray.add(interp1d(startDate, endDate, startArray[f], endArray[f]))
            }
            return coefArray
        }
        return null
    }

    private fun interp1d(x0: Double, x1: Double, y0: Double, y1: Double): DoubleArray {
        if (x0 == x1) return doubleArrayOf(0.0, 0.0)
        val k = (y1 - y0) / (x1 - x0)
        val b = y1 - k * x1
        return doubleArrayOf(b, k)
    }

    fun calculateCoeffs(point: Double): ArrayList<Double>? {
        if (!isInit) return null
        val resArray = ArrayList<Double>(195)

        val minStartYear = yearsMap.data.keys.min()
        val maxStartYear = yearsMap.data.keys.max()

        val startYear = ((point.toInt() / 5) * 5).takeIf { it >= minStartYear } ?: minStartYear
        val endYear = startYear + 5
        val startArrayRaw = yearsMap.data[startYear] ?: yearsMap.data[maxStartYear] ?: return null
        val startArray = (0..194).map {
            startArrayRaw.getOrElse(it) { 0.0 }
        }.toDoubleArray()
        val endArrayRaw = yearsMap.data[endYear]
        val endArray = endArrayRaw?.let {
            (0..194).map {
                endArrayRaw.getOrElse(it) { 0.0 }
            }.toDoubleArray()
        } ?: run {
            startArray.mapIndexed { index, d ->
                d + yearsMap.sv.getOrElse(index) { 0.0 }
            }.toDoubleArray()
        }
        yearsMap.startDate = startYear.toDouble()
        yearsMap.endDate = endYear.toDouble()
        yearsMap.startArray = startArray
        yearsMap.endArray = endArray

        val coefArray = getFuncArray()
        if (coefArray.isNullOrEmpty()) return null

        for (pol in coefArray) {
            if (pol.size < 2) return null
            var coef = 0.0
            for (mul in pol.indices) {
                coef += pol[mul] * point.pow(mul)
            }
            resArray.add(coef)
        }
        return resArray
    }

    fun init(files: Map<String, () -> InputStream>) {
        if (isInit) return

        fun readStream(stream: InputStream): List<Double> =
            stream.bufferedReader().lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { it.replace(",", ".").toDouble() }
                .toList()

        val sv = files.entries.firstOrNull { it.key.equals("sv", ignoreCase = true) }
            ?.value
            ?.invoke()
            ?.use { readStream(it) }
            ?: emptyList()

        val data = files.mapNotNull { (name, stream) ->
            name.toIntOrNull()?.let { year ->
                year to stream.invoke().use { readStream(it) }
            }
        }.toMap()

        yearsMap = YearsMap(data, sv)
        isInit = true
    }

    data class YearsMap(
        val data: Map<Int, List<Double>> = mapOf(),
        val sv: List<Double> = emptyList(),
        var startArray: DoubleArray? = null,
        var endArray: DoubleArray? = null,
        var startDate: Double? = null,
        var endDate: Double? = null,
    )
}