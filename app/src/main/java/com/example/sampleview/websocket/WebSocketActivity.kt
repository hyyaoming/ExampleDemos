package com.example.sampleview.websocket

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.R
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.IOException
import java.net.InetSocketAddress

class WebSocketActivity : AppCompatActivity() {

    companion object {
        const val TAG = "WebSocketDemo"
    }

    private var webSocketServer: MyWebSocketServer? = null
    private var clientWebSocket: WebSocket? = null
    private lateinit var tvLog: TextView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_socket)

        tvLog = findViewById(R.id.tvLog)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        startServer()

        startClient()

        btnSend.setOnClickListener {
            val msg = etMessage.text.toString()
            if (msg.isNotEmpty()) {
                clientWebSocket?.send(msg)
                appendLog("客户端发送: $msg")
                etMessage.setText("")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopClient()
        stopServer()
    }

    private fun appendLog(text: String) {
        runOnUiThread {
            tvLog.append("$text\n")
        }
    }

    // --- 服务端实现 ---
    inner class MyWebSocketServer(port: Int) : WebSocketServer(InetSocketAddress(port)) {

        override fun onOpen(conn: org.java_websocket.WebSocket?, handshake: ClientHandshake?) {
            Log.d(TAG, "服务端: 客户端已连接")
            appendLog("服务端: 客户端已连接")
            conn?.send("欢迎连接服务端！")
        }

        override fun onClose(conn: org.java_websocket.WebSocket?, code: Int, reason: String?, remote: Boolean) {
            Log.d(TAG, "服务端: 连接关闭，原因：$reason")
            appendLog("服务端: 连接关闭，原因：$reason")
        }

        override fun onMessage(conn: org.java_websocket.WebSocket?, message: String?) {
            Log.d(TAG, "服务端收到消息: $message")
            appendLog("服务端收到消息: $message")
            // 简单回声
            conn?.send("服务端回声: $message")
        }

        override fun onError(conn: org.java_websocket.WebSocket?, ex: java.lang.Exception?) {
            Log.e(TAG, "服务端异常", ex)
        }

        override fun onStart() {
            Log.d(TAG, "服务端启动成功")
        }
    }

    private fun startServer() {
        webSocketServer = MyWebSocketServer(8885)
        try {
            webSocketServer?.start()
            appendLog("服务端已启动，端口 8885")
            Log.d(TAG, "服务端启动成功")
        } catch (e: Exception) {
            Log.e(TAG, "服务端启动失败", e)
            appendLog("服务端启动失败：${e.message}")
        }
    }

    private fun stopServer() {
        try {
            webSocketServer?.stop()
            appendLog("服务端已停止")
        } catch (e: Exception) {
            Log.e(TAG, "服务端停止异常", e)
        }
    }

    private fun getLocalIpAddress(): String {
        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip = wm.connectionInfo.ipAddress
        return String.format(
            "%d.%d.%d.%d",
            ip and 0xff,
            ip shr 8 and 0xff,
            ip shr 16 and 0xff,
            ip shr 24 and 0xff
        )
    }


    // --- 客户端实现 ---
    private fun startClient() {
        val client = OkHttpClient()
        // 连接本机服务端时，手机端IP可能不一定是localhost，实际部署时要用手机局域网IP，示例先用localhost测试
        val ip = getLocalIpAddress()
        val url = "ws://$ip:8885/"
        val request = Request.Builder().url(url).build()

        clientWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "客户端: 连接成功")
                appendLog("客户端: 连接成功")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "客户端收到消息: $text")
                appendLog("客户端收到消息: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "客户端收到二进制消息")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "客户端连接关闭: $code $reason")
                appendLog("客户端连接关闭: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "客户端连接已关闭")
                appendLog("客户端连接已关闭")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "客户端连接失败", t)
                appendLog("客户端连接失败: ${t.message}")
            }
        })
    }

    private fun stopClient() {
        clientWebSocket?.close(1000, "App关闭")
    }
}
