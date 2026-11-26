package com.doccheck.app.util

import android.annotation.SuppressLint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.MotionEvent
import android.view.View

object GlassBlurHelper {

    /**
     * Applies a soft RenderEffect blur on Android 12+ (API 31+) to backdrop layers
     * behind floating glass bars or sheets, falling back to translucent fills on older APIs.
     */
    fun applyBackdropBlur(view: View, radiusPx: Float = 24f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setRenderEffect(
                RenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)
            )
        }
    }

    /**
     * Attaches press feedback that scales glass cards down to 0.98 on touch
     * while preserving standard click accessibility and ripple behavior.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun attachCardPressFeedback(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(90L).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
                }
            }
            false
        }
    }
}

