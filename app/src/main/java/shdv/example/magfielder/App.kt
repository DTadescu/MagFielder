package shdv.example.magfielder

import android.app.Application
import shdv.example.magfielder.data.IGRFCoef
import java.io.InputStream

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        IGRFCoef.init(getCoefFiles())
    }

    private fun getCoefFiles(): Map<String, () -> InputStream> {
        val listFileNames = assets.list("coef")?.mapNotNull { it } ?: emptyList()
        return listFileNames.associateWith { name -> { assets.open("coef/$name") } }
    }
}