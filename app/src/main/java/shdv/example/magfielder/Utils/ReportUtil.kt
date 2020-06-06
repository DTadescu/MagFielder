package shdv.example.magfielder.Utils

enum class ReportFormat(val value: Int){
TXT(0)
}

class ReportUtil(_format: ReportFormat, _key:String = "", _value:String = "") {
    private val format = _format
    var report = "$_key $_value"
    private set

    fun add(descriptor: String, value: String){
        when(format){
            ReportFormat.TXT -> addTxt(descriptor, value)
        }
    }

    private fun addTxt(descriptor: String, value: String){
        report += "\r\n$descriptor $value"
    }
}