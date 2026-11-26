package com.doccheck.app.ui.welcome

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.doccheck.app.R
import com.doccheck.app.data.local.SessionPreferences
import com.doccheck.app.databinding.FragmentWelcomeBinding
import com.doccheck.app.databinding.ItemWelcomeSlideBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WelcomeFragment : Fragment(R.layout.fragment_welcome) {

    @Inject
    lateinit var sessionPreferences: SessionPreferences

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!
    private var floatAnimator: ObjectAnimator? = null

    private data class WelcomeSlide(
        val titleRes: Int,
        val descRes: Int,
        val iconRes: Int,
        val tintColorRes: Int
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWelcomeBinding.bind(view)

        applyInsets()
        startFloatingArtAnimation()
        setupCarousel()
        setupListeners()
    }

    private fun applyInsets() {
        val baseBottom = resources.getDimensionPixelSize(R.dimen.screen_padding)
        ViewCompat.setOnApplyWindowInsetsListener(binding.welcomeRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.welcomeRoot.updatePadding(top = bars.top)
            binding.welcomeContent.updatePadding(bottom = baseBottom + bars.bottom)
            insets
        }
    }

    private fun startFloatingArtAnimation() {
        floatAnimator = ObjectAnimator.ofFloat(binding.ivWelcomeArt, View.TRANSLATION_Y, -14f, 14f).apply {
            duration = 4000L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun setupCarousel() {
        val slides = listOf(
            WelcomeSlide(
                R.string.welcome_slide1_title,
                R.string.welcome_slide1_desc,
                R.drawable.ic_copy_move,
                R.color.accent_pink_soft
            ),
            WelcomeSlide(
                R.string.welcome_slide2_title,
                R.string.welcome_slide2_desc,
                R.drawable.ic_ela,
                R.color.accent_mint_soft
            ),
            WelcomeSlide(
                R.string.welcome_slide3_title,
                R.string.welcome_slide3_desc,
                R.drawable.ic_sparkles,
                R.color.glass_inner
            )
        )

        binding.welcomePager.adapter = object : RecyclerView.Adapter<SlideVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideVH {
                val b = ItemWelcomeSlideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return SlideVH(b)
            }

            override fun onBindViewHolder(holder: SlideVH, position: Int) {
                val item = slides[position]
                holder.binding.tvSlideTitle.setText(item.titleRes)
                holder.binding.tvSlideDesc.setText(item.descRes)
                holder.binding.ivSlideIcon.setImageResource(item.iconRes)
                holder.binding.slideIconContainer.backgroundTintList =
                    holder.itemView.context.getColorStateList(item.tintColorRes)
            }

            override fun getItemCount(): Int = slides.size
        }

        renderDots(slides.size, 0)
        binding.welcomePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                renderDots(slides.size, position)
            }
        })
    }

    private fun renderDots(count: Int, activeIndex: Int) {
        val container = binding.dotsContainer
        container.removeAllViews()
        val density = resources.displayMetrics.density
        for (i in 0 until count) {
            val dot = ImageView(requireContext()).apply {
                setImageResource(if (i == activeIndex) R.drawable.dot_active else R.drawable.dot_inactive)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (4 * density).toInt()
                    marginEnd = (4 * density).toInt()
                }
            }
            container.addView(dot)
        }
    }

    private fun setupListeners() {
        binding.btnSkip.setOnClickListener { completeAndNavigate(startTab = 0) }
        binding.btnAlreadyAccount.setOnClickListener { completeAndNavigate(startTab = 0) }
        binding.btnGetStarted.setOnClickListener { completeAndNavigate(startTab = 1) }
    }

    private fun completeAndNavigate(startTab: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionPreferences.setOnboardingCompleted(true)
            val action = WelcomeFragmentDirections.actionWelcomeToAuth(startTab = startTab)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        floatAnimator?.cancel()
        floatAnimator = null
        _binding = null
        super.onDestroyView()
    }

    private class SlideVH(val binding: ItemWelcomeSlideBinding) : RecyclerView.ViewHolder(binding.root)
}

