package com.example.sampleview.eventtracker.monitor

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.sampleview.AppLogger
import com.example.sampleview.eventtracker.logger.TrackerLogger
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor.init
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor.isNetworkAvailable
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor.networkFlow
import com.example.sampleview.eventtracker.monitor.NetworkStateMonitor.unregister
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * **NetworkStateMonitor** - 网络状态监控器
 *
 * 该对象用于监听设备网络状态变化，可作为事件上传策略或其他业务逻辑的依据。
 *
 * ## 功能
 * 1. 首次初始化时获取当前网络状态；
 * 2. 通过 [ConnectivityManager.NetworkCallback] 监听网络可用/断开事件；
 * 3. 提供 [networkFlow] 可订阅网络状态变化；
 * 4. 提供同步快照 [isNetworkAvailable]，外部可随时获取当前网络状态。
 *
 * ## 注意事项
 * - 需在应用启动或需要监听网络时调用 [init] 进行初始化；
 * - [networkFlow] 使用 [MutableSharedFlow]，订阅时不会立即发射上一次状态；
 * - 可通过 [unregister] 注销回调，避免内存泄漏。
 */
object NetworkStateMonitor {

    /** 内部网络状态快照 */
    private var _networkAvailable: Boolean = true

    /** 当前网络状态快照，只读，外部不可修改 */
    val isNetworkAvailable: Boolean
        get() = _networkAvailable

    /** 内部可变流，用于广播网络状态变化 */
    private val _networkFlow = MutableSharedFlow<Boolean>()

    /** 对外只读共享流，协程订阅网络状态变化 */
    val networkFlow = _networkFlow.asSharedFlow()

    /** 标记是否已初始化，防止重复注册回调 */
    private var initialized = false

    /** 网络回调实例，用于注销 */
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /**
     * 初始化网络状态监控
     *
     * - 需在应用启动或需要监听网络时调用一次
     * - 会注册 [ConnectivityManager.NetworkCallback] 来监听网络状态变化
     *
     * @param context [Context] 实例
     * @param scope 协程作用域，用于发射网络状态变化事件
     */
    fun init(context: Context, scope: CoroutineScope) {
        if (initialized) return
        initialized = true

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 获取当前网络状态快照
        val activeNetwork = cm.activeNetwork
        val capabilities = cm.getNetworkCapabilities(activeNetwork)
        val available = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _networkAvailable = available
        TrackerLogger.logger.log("初始网络状态: $available")

        // 构建网络请求监听 WIFI 和蜂窝网络
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        // 注册网络回调
        val callBack = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (!_networkAvailable) {
                    _networkAvailable = true
                    TrackerLogger.logger.log("网络恢复连接")
                    scope.launch { _networkFlow.emit(true) }
                }
            }

            override fun onLost(network: Network) {
                if (_networkAvailable) {
                    _networkAvailable = false
                    TrackerLogger.logger.log("网络断开连接")
                    scope.launch { _networkFlow.emit(false) }
                }
            }
        }

        cm.registerNetworkCallback(request, callBack)
        this.networkCallback = callBack
    }

    /**
     * 注销网络状态监听，避免内存泄漏
     *
     * - 若不再需要监听网络变化，应调用该方法
     * - 会将已注册的 [ConnectivityManager.NetworkCallback] 注销
     *
     * @param context [Context] 实例
     */
    fun unregister(context: Context) {
        networkCallback?.let { cb ->
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(cb)
            networkCallback = null
            initialized = false
            AppLogger.d("EventTracker", "网络状态监听已注销")
        }
    }
}
