package com.example.sampleview.eventtracker.interceptor

import com.example.sampleview.eventtracker.model.Event

/**
 * 事件拦截器接口，用于在事件上报前进行修改或拦截处理。
 *
 * 通过实现 [EventInterceptor]，你可以在事件被最终上传前：
 *  - 添加或修改事件的公共属性（例如 activityPath、用户信息等）
 *  - 根据条件过滤事件（返回 null 表示拦截该事件，不上报）
 *
 * 通常配合 [EventInterceptorChain] 使用，通过链式调用确保多个拦截器依次生效。
 */
fun interface EventInterceptor {
    /**
     * 拦截事件。
     *
     * @param chain 当前拦截器链上下文，通过 chain.proceed() 获取下一个拦截器处理后的事件
     * @return 返回修改后的 [Event] 对象，如果返回 null 则表示拦截该事件，不再上报
     */
    suspend fun intercept(chain: EventInterceptorChain): Event?
}
