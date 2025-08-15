package com.example.sampleview.permission.util

import android.os.Build
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Method
import java.util.Properties

/**
 * 设备相关工具类。
 *
 * 主要用于检测设备的系统版本信息，特别是针对小米 MIUI 和魅族 Flyme 系统的判断。
 * 通过读取系统属性或系统文件获取版本信息，并提供设备类型判断方法。
 */
object DeviceUtil {

    /**
     * 小米 MIUI 版本号对应的系统属性键名
     */
    private const val KEY_MIUI_VERSION_NAME = "ro.miui.ui.version.name"

    /**
     * 魅族 Flyme 版本号对应的系统属性键名
     */
    private const val KEY_FLYME_VERSION_NAME = "ro.build.display.id"

    /**
     * Flyme 系统标识关键字
     */
    private const val FLYME: String = "flyme"

    /**
     * 魅族部分机型的 BOARD 名称数组
     */
    private val MEIZUBOARD = arrayOf("m9", "M9", "mx", "MX")

    /**
     * 缓存的小米 MIUI 版本号字符串，全部小写
     */
    var sMiuiVersionName: String? = null
        private set

    /**
     * 缓存的魅族 Flyme 版本号字符串，全部小写
     */
    var sFlymeVersionName: String? = null
        private set

    init {
        val properties = Properties()

        // Android 8.0（Oreo）以下版本，可以直接读取 /system/build.prop 文件获取系统属性
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            try {
                FileInputStream(File(Environment.getRootDirectory(), "build.prop")).use { input ->
                    properties.load(input)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 通过反射调用 android.os.SystemProperties#get(String) 方法获取系统属性
        try {
            val clzSystemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = clzSystemProperties.getDeclaredMethod("get", String::class.java)

            sMiuiVersionName = getLowerCaseName(properties, getMethod, KEY_MIUI_VERSION_NAME)
            sFlymeVersionName = getLowerCaseName(properties, getMethod, KEY_FLYME_VERSION_NAME)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 判断当前设备是否为魅族手机。
     * 通过判断 BOARD 名称是否属于魅族机型，或系统版本名是否包含 Flyme 标识。
     *
     * @return true 如果是魅族手机，否则 false
     */
    fun isMeizu(): Boolean {
        return isPhone(MEIZUBOARD) || isFlyme()
    }

    /**
     * 判断当前设备的 BOARD 名称是否在指定数组中。
     *
     * @param boards 目标 BOARD 名称数组
     * @return true 如果当前设备 BOARD 名称匹配，否则 false
     */
    fun isPhone(boards: Array<String>): Boolean {
        val board = Build.BOARD ?: return false
        return boards.any { it == board }
    }

    /**
     * 判断当前系统是否为魅族 Flyme 系统。
     *
     * @return true 如果 Flyme 版本名非空且包含 "flyme" 字符串，否则 false
     */
    fun isFlyme(): Boolean {
        return !sFlymeVersionName.isNullOrEmpty() && sFlymeVersionName!!.contains(FLYME)
    }

    /**
     * 获取指定系统属性键对应的属性值（小写）。
     * 先从 Properties 里获取，若不存在则反射调用 SystemProperties#get。
     *
     * @param properties 已加载的系统属性集合（可为空）
     * @param getMethod  反射得到的 SystemProperties#get 方法
     * @param key        属性键名
     * @return 属性值的小写字符串，获取失败返回 null
     */
    private fun getLowerCaseName(properties: Properties, getMethod: Method, key: String): String? {
        return try {
            var value = properties.getProperty(key)
            if (value.isNullOrEmpty()) {
                value = getMethod.invoke(null, key) as? String
            }
            value?.lowercase()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
