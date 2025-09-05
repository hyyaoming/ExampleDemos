package com.example.sampleview.voc.data.model

/**
 * 问卷场景枚举，用于标识问卷所属的业务场景。
 *
 * @property scene 场景标识，用于逻辑判断和数据存储。
 * @property desc 场景描述，用于显示或调试。
 */
enum class VocScene(val scene: String, val desc: String) {

    /** 应用冷启动场景 */
    APP_COLD_LAUNCH("coldLaunch", "应用冷启动"),

    /** 用户取消订单场景 */
    CANCEL_ORDER("cancelOrder", "取消订单")
}
