package com.example.sampleview.file

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.AppLogger
import com.example.sampleview.R
import java.io.IOException

class FileActivity : AppCompatActivity(R.layout.activity_file) {
    private val fileName = "login.txt"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btnWrite).setOnClickListener {
            val writeResult = writeLoginFile(this)
            AppLogger.d("FileActivity", "写入成功:$writeResult")
        }
        findViewById<Button>(R.id.btnRead).setOnClickListener {
            val result = isFileExistsInDownloads(this)
            AppLogger.d("FileActivity", "文件存在:$result")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun isFileExistsInDownloads(context: Context): Boolean {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(fileName)

        context.contentResolver.query(
            collection, arrayOf(MediaStore.MediaColumns._ID), selection, selectionArgs, null
        )?.use { cursor ->
            return cursor.count > 0
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun writeLoginFile(context: Context): Boolean {
        val content = "This is a login file.\nGenerated at: ${System.currentTimeMillis()}"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val fileUri = resolver.insert(collection, contentValues) ?: return false

        try {
            resolver.openOutputStream(fileUri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            } ?: return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(fileUri, contentValues, null, null)

        return true
    }


}