package com.example.sampleview.popupmanager

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SampleDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setTitle("Fragment Popup")
            .setMessage("Priority 8")
            .setPositiveButton("OK") { _, _ -> }
            .create()
    }
}
