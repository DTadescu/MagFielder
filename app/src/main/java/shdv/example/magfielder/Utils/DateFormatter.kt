package shdv.example.magfielder.Utils

import android.app.DatePickerDialog
import android.content.Context
import shdv.example.magfielder.data.ModelException
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

enum class DateFormat(val value: String) {
    DMY("dd/MM/yyyy"), MDY("MM/dd/yyyy"), YMD("yyyy/MM/dd");
    companion object{
        fun getFormat(x: String): DateFormat {
            for (f in values()){
                if(x == f.value) return f
            }
            return DMY
        }
    }

}

class UserDate(_day: Int, _month: Int, _year:Int){
    var day:Int = 1
        set(value) {
           field = if (value in 1..30) value
            else 1
        }

    var month:Int = 1
        set(value) {
            field = if (value in 1..12) value
            else 1
        }

    var year:Int = 2020
        set(value) {
            field = if (value in 2020..2099) value
            else 2020
        }

    init {
        day = _day
        month = _month
        year = _year
    }
}

class DateFormatter(_format: DateFormat) {
    val format = _format

    fun getDecimalDate(_date: String):Double{
        try{
            val date = getDateFromString(_date)
            var dayCount = date.day
            for(m in 1 until date.month){
                dayCount += getDayCountbyMonth(m, date.year)
            }
            return if(date.year%4 == 0) date.year + dayCount/366.0
                    else date.year + dayCount/365.0

        }
        catch (e:Exception){
            throw ModelException("Incorrect Date.")
        }

    }



    fun getCurrentDate():String{
        val sdf = SimpleDateFormat(format.value)
        return sdf.format(Date())
    }

    fun changeDate(context: Context, listener: DatePickerDialog.OnDateSetListener){
        val date = getDateFromString(getCurrentDate())
        val dPD = DatePickerDialog(context, listener, date.year, date.month-1, date.day).show()
    }

    fun getStringFromDate(_date: UserDate):String{
        return when(format){
            DateFormat.DMY ->{
                "${formatNumber(_date.day, 2)}/${formatNumber(_date.month, 2)}/${formatNumber(_date.year, 4)}"
            }
            DateFormat.MDY ->{
                "${formatNumber(_date.month, 2)}/${formatNumber(_date.day, 2)}/${formatNumber(_date.year, 4)}"
            }
            DateFormat.YMD ->{
                "${formatNumber(_date.year, 4)}/${formatNumber(_date.month, 2)}/${formatNumber(_date.day, 2)}"
            }
        }
    }

    private fun getDayCountbyMonth(month: Int, year: Int):Int{
        when(month){
            1, 3, 5, 7, 8, 10, 12 -> return 31
            4, 6, 9, 11 -> return 30
            2->{
                return if(year%4 == 0) 29
                else 28
            }
            else -> return 30
        }
    }

    fun getDateFromString(dString:String):UserDate{
        try {
            var day = 0;
            var month = 0;
            var year = 0;
            val dateSplit = dString.split('/')
            if(dateSplit.size != 3)
                throw ModelException("Incorrect Date.")
            when(format){
                DateFormat.DMY ->{
                    day = dateSplit[0].toInt()
                    month = dateSplit[1].toInt()
                    year = dateSplit[2].toInt()
                }
                DateFormat.MDY ->{
                    day = dateSplit[1].toInt()
                    month = dateSplit[0].toInt()
                    year = dateSplit[2].toInt()
                }
                DateFormat.YMD ->{
                    day = dateSplit[2].toInt()
                    month = dateSplit[1].toInt()
                    year = dateSplit[0].toInt()
                }
            }
            return UserDate(day, month, year)
        }
        catch (e:Exception){
            e.printStackTrace()
            throw ModelException("Incorrect Date.")
        }

    }



    private fun formatNumber(value: Int, sign: Int):String{
        var result = value.toString()
        if (result.length < sign){
            for (i in result.length until sign){
                result = "0$result"
            }
        }
        return result
    }
}