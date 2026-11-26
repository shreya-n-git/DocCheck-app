package com.doccheck.app.ui.history

import android.net.Uri
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.doccheck.app.R
import com.doccheck.app.data.model.ScanResult
import com.doccheck.app.data.repository.ScanRepository
import com.doccheck.app.databinding.FragmentHistoryBinding
import com.doccheck.app.databinding.ItemHistoryScanBinding
import com.doccheck.app.util.GlassBlurHelper
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    scanRepository: ScanRepository
) : ViewModel() {
    val scans: StateFlow<List<ScanResult>> = scanRepository.observeScanHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@AndroidEntryPoint
class HistoryFragment : Fragment(R.layout.fragment_history) {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHistoryBinding.bind(view)

        val baseBottom = resources.getDimensionPixelSize(R.dimen.bottom_scroll_clearance)
        ViewCompat.setOnApplyWindowInsetsListener(binding.historyRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.historyRoot.updatePadding(top = bars.top)
            binding.rvHistory.updatePadding(bottom = baseBottom + bars.bottom)
            insets
        }

        val adapter = HistoryAdapter { scan ->
            val action = HistoryFragmentDirections.actionHistoryToResult(scan.id)
            findNavController().navigate(action)
        }
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.scans.collect { list ->
                    binding.emptyHistoryCard.isVisible = list.isEmpty()
                    binding.rvHistory.isVisible = list.isNotEmpty()
                    adapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private class HistoryAdapter(
        private val onClick: (ScanResult) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        private var items: List<ScanResult> = emptyList()

        fun submitList(list: List<ScanResult>) {
            items = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemHistoryScanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            val ctx = holder.itemView.context
            holder.binding.ivThumb.load(Uri.parse(item.imageUri)) {
                crossfade(true)
                placeholder(R.drawable.bg_glass_card)
                error(R.drawable.bg_glass_card)
            }
            holder.binding.tvHistoryVerdict.setText(
                if (item.isTampered) R.string.result_verdict_tampered else R.string.result_verdict_clean
            )
            holder.binding.tvHistoryDate.text =
                DateFormat.format("MMM d, yyyy · h:mm a", Date(item.timestamp))
            holder.binding.tvHistoryScore.text = ctx.getString(R.string.result_score_fmt, item.tamperScore)
            holder.binding.tvHistoryScore.backgroundTintList = ContextCompat.getColorStateList(
                ctx,
                if (item.isTampered) R.color.accent_pink else R.color.accent_mint
            )
            holder.binding.historyItemCard.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size

        class VH(val binding: ItemHistoryScanBinding) : RecyclerView.ViewHolder(binding.root) {
            init {
                GlassBlurHelper.attachCardPressFeedback(binding.historyItemCard)
            }
        }
    }
}

