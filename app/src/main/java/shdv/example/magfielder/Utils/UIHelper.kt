package shdv.example.magfielder.Utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.text.Layout
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import shdv.example.magfielder.R

object UIHelper {

    fun getSimpleDialog(activity: Activity, resource: Int):Dialog{
        val myDialog = Dialog(activity, R.style.DialogWindowStyle)
        myDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        myDialog.setContentView(resource)

        return myDialog

    }


}