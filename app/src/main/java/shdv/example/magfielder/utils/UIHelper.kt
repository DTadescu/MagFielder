package shdv.example.magfielder.utils

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.Window
import shdv.example.magfielder.R

object UIHelper {

    fun getSimpleDialog(activity: Activity, view: View): Dialog {
        val myDialog = Dialog(activity, R.style.DialogWindowStyle)
        myDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        myDialog.setContentView(view)

        return myDialog

    }


}