package com.example.sampleview.permission

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.AppLogger
import com.example.sampleview.R
import com.example.sampleview.permission.api.PermissionForwardCallback
import com.example.sampleview.permission.api.PermissionForwardScope
import com.example.sampleview.permission.impl.DefaultPermissionDescProvider
import com.example.sampleview.permission.impl.PermissionUiHintSlice
import com.example.sampleview.permission.impl.PermissionXBuilder
import com.example.sampleview.permission.model.PermissionHost
import com.example.yann.waveapplication.collectWithScope

class PermissionActivity : AppCompatActivity(R.layout.permission_activity) {

    private val requestWriteSettingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        runOnUiThread {
            if (PermissionClient.hasPermissions(this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this@PermissionActivity, "授权成功", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        findViewById<Button>(R.id.btnRequest).setOnClickListener {
            val builder = PermissionXBuilder(PermissionHost.ActivityHost(this)).setRequestPermissions(Manifest.permission.RECORD_AUDIO)
                .setLifecycleSlice(PermissionUiHintSlice(DefaultPermissionDescProvider())).setForwardCallback(object : PermissionForwardCallback {
                    override fun onForwardToSettings(deniedList: List<String>, context: Context, scope: PermissionForwardScope) {
                        val dialog = DefaultRationaleDialog(
                            this@PermissionActivity,
                            deniedList,
                            "我们需要该权限哦，不能部分功能使用不了"
                        )
                        scope.showForwardToSettingsDialog { dialog }
                    }
                })
//            lifecycleScope.launch {
//                val result = PermissionClient.from(builder).request()
//                if (result.allGranted) {
//                    Toast.makeText(this@PermissionActivity, "授权成功", Toast.LENGTH_SHORT).show()
//                } else {
//                    Toast.makeText(this@PermissionActivity, "授权失败", Toast.LENGTH_SHORT).show()
//                }
//            }
            PermissionClient.from(builder).requestFlow().collectWithScope(lifecycleScope) { result ->
                AppLogger.d("PermissionActivity", "永远拒绝:${shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)}")
                if (result.allGranted) {
                    Toast.makeText(this@PermissionActivity, "授权成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PermissionActivity, "授权失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}