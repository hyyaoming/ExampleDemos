package com.example.sampleview.behavior

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

fun KClass<*>.getDeclaredFieldOrNull(vararg names: String): Field? {
    for (name in names) {
        try {
            return this.java.getDeclaredField(name).apply {
                isAccessible = true
            }
        } catch (e: NoSuchFieldException) {
            // 忽略，尝试下一个字段名
        }
    }
    return null
}


class DeclaredField<T: Any?> (name: String) {
    private val field = BottomSheetBehavior::class.getDeclaredFieldOrNull(name)

    operator fun <V: View> getValue(bottomSheetBehavior: BottomSheetBehavior<V>, property: KProperty<*>): T? {
        @Suppress("UNCHECKED_CAST")
        return field?.get(bottomSheetBehavior) as? T
    }

    operator fun <V: View> setValue(bottomSheetBehavior: BottomSheetBehavior<V>, property: KProperty<*>, value: T) {
        field?.set(bottomSheetBehavior, value)
    }
}