package com.example.sampleview.glide

import android.content.Intent
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.text.TextUtils
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.sampleview.R
import com.example.sampleview.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLConnection
import java.security.MessageDigest

class GlideActivity : AppCompatActivity(R.layout.activty_glide) {

    private val imageUrl = "https://picsum.photos/800" // 示例图片
    private val videoUrl =
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4" // 示例视频

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 600)
        }

        val videoThumbView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 600)
        }

        layout.addView(imageView)
        layout.addView(videoThumbView)
        setContentView(layout)

        // 下载并展示图片
        downloadMediaFile(imageUrl) { file ->
            Glide.with(this@GlideActivity).load(file).into(imageView)
            imageView.setOnClickListener {
                openFileWithSystem(file)
            }
        }
        // 下载并展示视频缩略图
        downloadMediaFile(videoUrl) { file ->
            Glide.with(this@GlideActivity).load(file).into(videoThumbView)
            videoThumbView.setOnClickListener {
                openFileWithSystem(file)
            }
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val result = digest.digest(input.toByteArray())
        return result.joinToString("") { "%02x".format(it) }
    }

    private fun downloadMediaFile(url: String, onFileReady: (File) -> Unit) {
        val file = File(filesDir, "preview" + File.separator + md5(url))
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        lifecycleScope.launch(Dispatchers.IO) {
            if (!file.exists()) {
                val future = Glide.with(this@GlideActivity).downloadOnly().load(url).submit()
                val downloaded = future.get()
                val mimeType = detectMediaType(downloaded)
                Timber.d("mimeType:${mimeType}")
                downloaded.copyTo(file, overwrite = true)
                Glide.with(this@GlideActivity).clear(future)
            }
            withContext(Dispatchers.Main) {
                onFileReady(file)
            }
        }
    }


    enum class MediaType {
        IMAGE, VIDEO, OTHER
    }

    /**
     * 根据文件判断媒体类型，返回 MediaType 或 null（无法判断）
     */
    fun detectMediaType(file: File): MediaType? {
        // 1. 优先根据扩展名判断
        val ext = file.extension.lowercase()
        val imageExt = listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic")
        val videoExt = listOf("mp4", "avi", "mov", "wmv", "flv", "mkv", "webm")

        if (ext in imageExt) return MediaType.IMAGE
        if (ext in videoExt) return MediaType.VIDEO

        // 通过文件流检测 mimeType
        val mimeType = try {
            file.inputStream().use { input ->
                URLConnection.guessContentTypeFromStream(input) ?: ""
            }
        } catch (e: Exception) {
            ""
        }

        if (mimeType != null) {
            if (mimeType.startsWith("image")) return MediaType.IMAGE
            if (mimeType.startsWith("video")) return MediaType.VIDEO
            return MediaType.OTHER
        }

        // 3. MIME 也无法判断时，尝试用 MediaMetadataRetriever 判断是否是视频
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            retriever.release()
            if (hasVideo == "yes") MediaType.VIDEO else null
        } catch (e: Exception) {
            null
        }
    }



    private fun openFileWithSystem(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        var mimeType = contentResolver.getType(uri) ?: ""
        if (mimeType.startsWith("video")) {
            mimeType = "video/*"
        } else if (mimeType.startsWith("image")) {
            mimeType = "image/*"
        }
        if (!TextUtils.isEmpty(mimeType)) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        }
    }

}