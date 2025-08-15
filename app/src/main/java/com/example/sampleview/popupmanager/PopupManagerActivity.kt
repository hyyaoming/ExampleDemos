package com.example.sampleview.popupmanager

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.R

class PopupManagerActivity : AppCompatActivity(R.layout.activity_popup_manager) {
    private val popupManager = PopupManager(lifecycleScope)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.btnAddPopup).setOnClickListener {
            popupManager.enqueue(this, AlertPopup(priority = 1, tag = "alert1") {
                AlertDialog.Builder(this).setTitle("Alert 1").setMessage("Priority 1").setPositiveButton("OK", null).create()
            })

            popupManager.enqueue(this, AlertPopup(priority = 2, tag = "alert2") {
                AlertDialog.Builder(this).setTitle("Alert 2").setMessage("Priority 2").setPositiveButton("OK", null).create()
            })

            popupManager.enqueue(this, AlertPopup(priority = 3, tag = "alert3") {
                AlertDialog.Builder(this).setTitle("Alert 3").setMessage("Priority 3").setPositiveButton("OK", null).create()
            })

            popupManager.enqueue(this, FragmentPopup(priority = 4, tag = "fragment1", supportFragmentManager) {
                SampleDialogFragment()
            })

        }
    }

}