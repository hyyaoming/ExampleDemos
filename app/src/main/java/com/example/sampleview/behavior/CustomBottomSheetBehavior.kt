package com.example.sampleview.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import java.lang.ref.WeakReference

class CustomBottomSheetBehavior<T> @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BottomSheetBehavior<View>(context, attrs) {

    private val nestedScrollingChildRefs = mutableListOf<WeakReference<View>>()

    private var nestedScrollingChildRef by DeclaredField<WeakReference<View?>?>("nestedScrollingChildRef")

    private fun findScrollingChildren(view: View) {
        if (view.isVisible.not()) {
            return
        }

        if (ViewCompat.isNestedScrollingEnabled(view)) {
            nestedScrollingChildRefs.add(WeakReference(view))
        }

        if (view is ViewGroup) {
            view.children.forEach {
                findScrollingChildren(it)
            }
        }
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout, child: View, layoutDirection: Int
    ): Boolean {
        return super.onLayoutChild(parent, child, layoutDirection).also {
            if (nestedScrollingChildRefs.isEmpty()) {
                findScrollingChildren(child)
            }
        }
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout, child: View, event: MotionEvent
    ): Boolean {
        nestedScrollingChildRefs.mapNotNull { it.get() }
            .find { parent.isPointInChildBounds(it, event.x.toInt(), event.y.toInt()) }
            .also { nestedScrollingChildRef = WeakReference(it) }

        return super.onInterceptTouchEvent(parent, child, event)
    }
}