package com.example.cs442katie.ui


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment

import com.example.cs442katie.R

/**
 * A simple [Fragment] subclass.
 */
class VerifyDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val contentView = activity?.layoutInflater?.inflate(R.layout.fragment_verify_dialog, null)
        builder.setView(contentView)
        val dialog = builder.create()
        return dialog
    }




}
