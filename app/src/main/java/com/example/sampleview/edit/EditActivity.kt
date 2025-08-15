package com.example.sampleview.edit

import android.os.Bundle
import android.text.InputFilter
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.R

class EditActivity : AppCompatActivity(R.layout.activity_edit) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val editText: EditText = findViewById(R.id.editText)
        val filter = InputFilter { source, start, end, _, _, _ ->
            val pattern = Regex("^[a-zA-Z\\u4e00-\\u9fa5]+$")
            val filtered = source.subSequence(start, end).toString()
            if (pattern.matches(filtered)) null else ""
        }
        editText.filters = arrayOf(filter)

    }

}