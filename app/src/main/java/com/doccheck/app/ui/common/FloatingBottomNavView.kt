package com.doccheck.app.ui.common

import android.content.Context
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.doccheck.app.R
import com.doccheck.app.databinding.ViewFloatingBottomNavBinding
import com.doccheck.app.util.GlassBlurHelper

class FloatingBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewFloatingBottomNavBinding.inflate(LayoutInflater.from(context), this, true)

    private var selectedDestId: Int = R.id.homeFragment
    private var onTabSelectedListener: ((Int) -> Unit)? = null

    private data class TabSlot(
        val destId: Int,
        val container: LinearLayout,
        val icon: ImageView,
        val label: TextView
    )

    private val slots: List<TabSlot> by lazy {
        listOf(
            TabSlot(R.id.homeFragment, binding.tabHome, binding.iconHome, binding.labelHome),
            TabSlot(R.id.chatFragment, binding.tabChat, binding.iconChat, binding.labelChat),
            TabSlot(R.id.historyFragment, binding.tabHistory, binding.iconHistory, binding.labelHistory),
            TabSlot(R.id.settingsFragment, binding.tabSettings, binding.iconSettings, binding.labelSettings)
        )
    }

    init {
        GlassBlurHelper.applyBackdropBlur(binding.navBlurBackdrop, 24f)
        slots.forEach { slot ->
            slot.container.setOnClickListener {
                if (selectedDestId != slot.destId) {
                    setSelectedDestination(slot.destId, animate = true)
                    onTabSelectedListener?.invoke(slot.destId)
                }
            }
        }
        setSelectedDestination(R.id.homeFragment, animate = false)
    }

    fun setOnTabSelectedListener(listener: (Int) -> Unit) {
        onTabSelectedListener = listener
    }

    fun setSelectedDestination(destId: Int, animate: Boolean = true) {
        selectedDestId = destId
        if (animate) {
            val transition = AutoTransition().apply { duration = 220 }
            TransitionManager.beginDelayedTransition(binding.navContainer, transition)
        }
        val activeInk = ContextCompat.getColor(context, R.color.nav_active_content)
        val inactiveInk = ContextCompat.getColor(context, R.color.ink)
        val hPadActive = (16 * resources.displayMetrics.density).toInt()
        val hPadInactive = (12 * resources.displayMetrics.density).toInt()

        slots.forEach { slot ->
            val isActive = slot.destId == destId
            slot.container.setBackgroundResource(
                if (isActive) R.drawable.bg_pill_black else R.drawable.bg_glass_circle
            )
            slot.container.setPadding(
                if (isActive) hPadActive else hPadInactive,
                0,
                if (isActive) hPadActive else hPadInactive,
                0
            )
            slot.icon.setColorFilter(if (isActive) activeInk else inactiveInk)
            slot.label.isVisible = isActive
            slot.container.isSelected = isActive
        }
    }
}

