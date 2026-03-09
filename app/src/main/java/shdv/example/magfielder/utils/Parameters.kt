package shdv.example.magfielder.utils

enum class Language(val value: String) {
    RUSSIAN("ru"), ENGLISH("en")
}

enum class Model(val value: String) {
    IGRF("IGRF"), WMM("WMM")
}

enum class EarthShape(val value: String) {
    SPHERE("SPHERE"), ELLIPSOID("ELLIPSOID")
}