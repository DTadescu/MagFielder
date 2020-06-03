package shdv.example.magfielder.Utils

import kotlinx.android.synthetic.main.activity_main.view.*
import shdv.example.magfielder.R

enum class Language(val value: String) {
    RUSSIAN("ru"), ENGLISH("en")
}

enum class Model(val value: String) {
    IGRF("IGRF"), WMM("WMM")
}

enum class EarthShape(val value: String){
    SPHERE("SPHERE"), ELLIPSOID("ELLIPSOID")
}