package shdv.example.magfielder.utils

fun String.toLatitude(): String {
    return this.replace("N", "", ignoreCase = true)
        .replace("S", "-", ignoreCase = true)
}

fun String.toLongitude(): String {
    return this.replace("E", "", ignoreCase = true)
        .replace("W", "-", ignoreCase = true)
}