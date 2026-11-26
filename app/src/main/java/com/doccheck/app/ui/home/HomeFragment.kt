package com.doccheck.app.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.doccheck.app.R
import com.doccheck.app.data.model.ForensicModule
import com.doccheck.app.data.model.ForensicToolItem
import com.doccheck.app.databinding.FragmentHomeBinding
import com.doccheck.app.util.FileUtils
import com.doccheck.app.util.GlassBlurHelper
import com.doccheck.app.util.ImageValidationResult
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private var pendingModule: ForensicModule? = null

    private val pickMediaLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            handlePickedUri(uri)
        }
        pendingModule = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        applyInsets()
        setupCardsFeedback()
        setupToolkitGrid()
        setupListeners()
        observeViewModel()
    }

    private fun applyInsets() {
        val baseTop = (12 * resources.displayMetrics.density).toInt()
        val baseBottom = resources.getDimensionPixelSize(R.dimen.bottom_scroll_clearance)
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.homeContent.updatePadding(
                top = baseTop + bars.top,
                bottom = baseBottom + bars.bottom
            )
            insets
        }
    }

    private fun setupCardsFeedback() {
        GlassBlurHelper.attachCardPressFeedback(binding.heroCard)
        GlassBlurHelper.attachCardPressFeedback(binding.cardStatScanned)
        GlassBlurHelper.attachCardPressFeedback(binding.cardStatTampered)
        GlassBlurHelper.attachCardPressFeedback(binding.cardStatAccuracy)
    }

    private fun setupToolkitGrid() {
        val tools = listOf(
            ForensicToolItem(
                module = ForensicModule.COPY_MOVE,
                title = getString(R.string.module_copymove_title),
                description = getString(R.string.module_copymove_desc),
                iconRes = R.drawable.ic_copy_move,
                badgeTintRes = R.color.accent_pink_soft
            ),
            ForensicToolItem(
                module = ForensicModule.ELA,
                title = getString(R.string.module_ela_title),
                description = getString(R.string.module_ela_desc),
                iconRes = R.drawable.ic_ela,
                badgeTintRes = R.color.accent_mint_soft
            ),
            ForensicToolItem(
                module = ForensicModule.METADATA,
                title = getString(R.string.module_metadata_title),
                description = getString(R.string.module_metadata_desc),
                iconRes = R.drawable.ic_metadata,
                badgeTintRes = R.color.accent_lilac
            ),
            ForensicToolItem(
                module = ForensicModule.SIGNATURE,
                title = getString(R.string.module_signature_title),
                description = getString(R.string.module_signature_desc),
                iconRes = R.drawable.ic_signature,
                badgeTintRes = R.color.glass_inner
            )
        )
        binding.rvToolkit.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvToolkit.adapter = ForensicToolkitAdapter(tools) { item ->
            launchDocumentPicker(item.module)
        }
    }

    private fun setupListeners() {
        binding.btnUploadDocument.setOnClickListener {
            launchDocumentPicker(null)
        }
        binding.btnThemeToggle.setOnClickListener {
            viewModel.toggleDarkMode()
        }
        binding.btnNotifications.setOnClickListener {
            binding.notificationBadge.isVisible = false
            Snackbar.make(binding.root, getString(R.string.home_notifications_none), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun launchDocumentPicker(module: ForensicModule?) {
        pendingModule = module
        pickMediaLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    private fun handlePickedUri(uri: Uri) {
        when (FileUtils.validatePickedImage(requireContext(), uri)) {
            is ImageValidationResult.Valid -> {
                viewModel.analyzePickedImage(uri, pendingModule)
            }
            is ImageValidationResult.InvalidType -> {
                Snackbar.make(binding.root, getString(R.string.err_file_type), Snackbar.LENGTH_LONG).show()
            }
            is ImageValidationResult.TooLarge -> {
                Snackbar.make(binding.root, getString(R.string.err_file_size), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.stats.collect { stats ->
                        binding.tvStatScanned.text = stats.scanned.toString()
                        binding.tvStatTampered.text = stats.tampered.toString()
                        binding.tvStatAccuracy.text = getString(R.string.stat_accuracy_fmt, stats.accuracy)
                        binding.pbAccuracy.progress = stats.accuracy.toInt().coerceIn(0, 100)
                    }
                }
                launch {
                    viewModel.isDarkMode.collect { dark ->
                        binding.ivThemeIcon.setImageResource(
                            if (dark) R.drawable.ic_sun else R.drawable.ic_moon
                        )
                    }
                }
                launch {
                    viewModel.isAnalyzing.collect { analyzing ->
                        binding.analyzingOverlay.isVisible = analyzing
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is HomeEvent.OpenResult -> {
                                val action = HomeFragmentDirections.actionHomeToResult(event.scanId)
                                findNavController().navigate(action)
                            }
                            is HomeEvent.ShowError -> {
                                Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

