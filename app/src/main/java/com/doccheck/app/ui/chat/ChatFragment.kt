package com.doccheck.app.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.doccheck.app.R
import com.doccheck.app.databinding.FragmentChatBinding
import com.doccheck.app.util.FileUtils
import com.doccheck.app.util.ImageValidationResult
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment(R.layout.fragment_chat) {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private val args: ChatFragmentArgs by navArgs()

    private lateinit var chatAdapter: ChatAdapter

    private val attachLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            when (FileUtils.validatePickedImage(requireContext(), uri)) {
                is ImageValidationResult.Valid -> viewModel.attachAndAnalyzeImage(uri)
                is ImageValidationResult.InvalidType -> {
                    Snackbar.make(binding.root, getString(R.string.err_file_type), Snackbar.LENGTH_LONG).show()
                }
                is ImageValidationResult.TooLarge -> {
                    Snackbar.make(binding.root, getString(R.string.err_file_size), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChatBinding.bind(view)

        if (savedInstanceState == null) {
            viewModel.attachScanContext(args.scanContext)
        }

        applyInsets()
        setupRecycler()
        setupListeners()
        observeState()
    }

    private fun applyInsets() {
        val baseTop = (10 * resources.displayMetrics.density).toInt()
        val navClearance = (100 * resources.displayMetrics.density).toInt()
        val imePad = (12 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(binding.chatRoot) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeOpen = insets.isVisible(WindowInsetsCompat.Type.ime())

            binding.chatTopBar.updatePadding(top = baseTop + systemBars.top)
            binding.chatInputBar.updateLayoutParams<androidx.constraintlayout.widget.ConstraintLayout.LayoutParams> {
                bottomMargin = if (imeOpen) {
                    imeInsets.bottom + imePad
                } else {
                    navClearance + systemBars.bottom
                }
            }
            insets
        }
    }

    private fun setupRecycler() {
        chatAdapter = ChatAdapter { source ->
            SourceBottomSheetDialogFragment.newInstance(source)
                .show(childFragmentManager, "source_sheet")
        }
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        binding.btnChatBack.setOnClickListener {
            if (!findNavController().popBackStack()) {
                findNavController().navigate(R.id.homeFragment)
            }
        }

        binding.btnChatMenu.setOnClickListener { anchor ->
            val popup = PopupMenu(requireContext(), anchor)
            popup.menuInflater.inflate(R.menu.menu_chat_options, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_clear_chat -> {
                        viewModel.clearChat()
                        Snackbar.make(binding.root, getString(R.string.chat_cleared), Snackbar.LENGTH_SHORT).show()
                        true
                    }
                    R.id.action_export_chat -> {
                        exportChat()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        binding.btnDetachScan.setOnClickListener {
            viewModel.clearAttachedScan()
        }

        binding.btnAttachDoc.setOnClickListener {
            attachLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.etChatInput.doAfterTextChanged { text ->
            val hasText = !text.isNullOrBlank() && !viewModel.isSending.value
            updateSendButton(hasText)
        }

        binding.btnSend.setOnClickListener {
            val prompt = binding.etChatInput.text?.toString().orEmpty()
            if (prompt.isNotBlank() && !viewModel.isSending.value) {
                binding.etChatInput.setText("")
                viewModel.sendMessage(prompt)
            }
        }

        val chips = listOf(
            binding.chipSuggestion1,
            binding.chipSuggestion2,
            binding.chipSuggestion3,
            binding.chipSuggestion4
        )
        chips.forEach { chip ->
            chip.setOnClickListener {
                viewModel.sendMessage(chip.text.toString())
            }
        }
    }

    private fun updateSendButton(enabled: Boolean) {
        binding.btnSend.isEnabled = enabled
        binding.sendCircle.alpha = if (enabled) 1.0f else 0.4f
    }

    private fun exportChat() {
        val transcript = viewModel.buildExportTranscript()
        if (transcript.isBlank()) return
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("DocCheck Chat", transcript))
        Snackbar.make(binding.root, getString(R.string.chat_exported), Snackbar.LENGTH_SHORT).show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.messages.collect { msgs ->
                        val isEmpty = msgs.isEmpty()
                        binding.chatEmptyState.isVisible = isEmpty
                        binding.rvChatMessages.isVisible = !isEmpty
                        chatAdapter.submitList(msgs) {
                            if (msgs.isNotEmpty()) {
                                binding.rvChatMessages.scrollToPosition(msgs.size - 1)
                            }
                        }
                    }
                }
                launch {
                    viewModel.attachedScan.collect { scan ->
                        binding.attachedScanCard.isVisible = scan != null
                        if (scan != null) {
                            binding.ivAttachedThumb.load(Uri.parse(scan.imageUri)) {
                                crossfade(true)
                                placeholder(R.drawable.bg_glass_card)
                            }
                            binding.tvAttachedMeta.text =
                                getString(R.string.chat_attached_meta_fmt, scan.tamperScore)
                        }
                    }
                }
                launch {
                    viewModel.isSending.collect { sending ->
                        val hasText = !binding.etChatInput.text.isNullOrBlank()
                        updateSendButton(hasText && !sending)
                    }
                }
                launch {
                    viewModel.errorEvents.collect { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
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

