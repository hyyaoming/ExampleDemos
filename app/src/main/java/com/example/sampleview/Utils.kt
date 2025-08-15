package com.example.yann.waveapplication

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object Utils {

    fun dip2px(context: Context, dpValue: Int): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}

fun <T> Flow<T>.collectWithScope(scope: LifecycleCoroutineScope, block: suspend (T) -> Unit) {
    scope.launch {
        distinctUntilChanged().collect {
            block(it)
        }
    }
}

fun <T> Flow<T>.collectWithLifecycle(lifecycleOwner: LifecycleOwner, block: suspend (T) -> Unit) {
    lifecycleOwner.lifecycleScope.launch {
        /*** onStop → onStart 时会重新启动Flow并重新collect，哪怕值没变，也会触发一次 collect 块中的逻辑**/
        var lastValue: T? = null
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            this@collectWithLifecycle.distinctUntilChanged().collect {
                if (it != lastValue) {
                    lastValue = it
                    block(it)
                }
            }
        }
    }
}