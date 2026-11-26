package com.doccheck.app.ui.home

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.doccheck.app.R
import com.doccheck.app.data.model.Finding
import com.doccheck.app.data.model.ScanResult
import com.doccheck.app.data.model.Severity
import com.doccheck.app.data.repository.ScanRepository
import com.doccheck.app.databinding.FragmentResultBinding
import com.doccheck.app.databinding.ItemFindingBinding
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val scanRepository: ScanRepository
) : ViewModel() {

    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    fun loadScan(scanId: String) {
        viewModelScope.launch {
            _scanResult.value = scanRepository.getScanById(scanId)
        }
    }
}

@AndroidEntryPoint
class ResultFragment : Fragment(R.layout.fragment_result) {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ResultViewModel by viewModels()
    private val args: ResultFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentResultBinding.bind(view)

        val baseTop = (12 * resources.displayMetrics.density).toInt()
        val baseBottom = (32 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.resultScroll) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.resultContent.updatePadding(top = baseTop + bars.top, bottom = baseBottom + bars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.rvFindings.layoutManager = LinearLayoutManager(requireContext())

        viewModel.loadScan(args.scanId)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scanResult.collect { scan ->
                    if (scan != null) renderScan(scan)
                }
            }
        }
    }

    private fun renderScan(scan: ScanResult) {
        binding.ivDocumentPreview.load(Uri.parse(scan.imageUri)) {
            crossfade(true)
            placeholder(R.drawable.bg_glass_card)
            error(R.drawable.bg_glass_card)
        }
        binding.pbTamperScore.progress = scan.tamperScore
        binding.tvTamperScore.text = getString(R.string.result_score_fmt, scan.tamperScore)

        val indicatorColor = ContextCompat.getColor(
            requireContext(),
            if (scan.isTampered) R.color.accent_pink else R.color.accent_mint
        )
        binding.pbTamperScore.setIndicatorColor(indicatorColor)
        binding.tvVerdictBadge.setText(
            if (scan.isTampered) R.string.result_verdict_tampered else R.string.result_verdict_clean
        )
        binding.tvVerdictBadge.backgroundTintList = ContextCompat.getColorStateList(
            requireContext(),
            if (scan.isTampered) R.color.accent_pink else R.color.accent_mint
        )

        binding.rvFindings.adapter = FindingsAdapter(scan.findings)
        binding.btnAskAi.setOnClickListener {
            val action = ResultFragmentDirections.actionResultToChat(scanContext = scan)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private class FindingsAdapter(
        private val items: List<Finding>
    ) : RecyclerView.Adapter<FindingsAdapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemFindingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.binding.tvFindingModule.text = item.module
            holder.binding.tvFindingDesc.text = item.description
            holder.binding.tvSeverityBadge.text = item.severity.name
            val tint = when (item.severity) {
                Severity.HIGH -> R.color.accent_pink
                Severity.MEDIUM -> R.color.accent_lilac
                Severity.LOW -> R.color.accent_mint
            }
            holder.binding.tvSeverityBadge.backgroundTintList =
                ContextCompat.getColorStateList(holder.itemView.context, tint)
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemFindingBinding) : RecyclerView.ViewHolder(binding.root)
    }
}

