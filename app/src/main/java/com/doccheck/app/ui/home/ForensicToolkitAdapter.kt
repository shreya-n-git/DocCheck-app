package com.doccheck.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.doccheck.app.data.model.ForensicToolItem
import com.doccheck.app.databinding.ItemForensicToolBinding
import com.doccheck.app.util.GlassBlurHelper

class ForensicToolkitAdapter(
    private val items: List<ForensicToolItem>,
    private val onClick: (ForensicToolItem) -> Unit
) : RecyclerView.Adapter<ForensicToolkitAdapter.ToolViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemForensicToolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ToolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ToolViewHolder(
        private val binding: ItemForensicToolBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            GlassBlurHelper.attachCardPressFeedback(binding.toolkitCard)
        }

        fun bind(item: ForensicToolItem) {
            binding.tvToolTitle.text = item.title
            binding.tvToolDesc.text = item.description
            binding.ivToolIcon.setImageResource(item.iconRes)
            binding.iconCircle.backgroundTintList =
                ContextCompat.getColorStateList(binding.root.context, item.badgeTintRes)
            binding.toolkitCard.setOnClickListener { onClick(item) }
        }
    }
}

