package com.doccheck.app.ui.chat

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.doccheck.app.R
import com.doccheck.app.data.model.ChatMessage
import com.doccheck.app.data.model.ChatRole
import com.doccheck.app.data.model.Source
import com.doccheck.app.databinding.ItemChatAssistantBinding
import com.doccheck.app.databinding.ItemChatUserBinding
import com.doccheck.app.databinding.ItemSourceChipBinding

class ChatAdapter(
    private val onSourceClick: (Source) -> Unit
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_USER = 1
        private const val VIEW_ASSISTANT = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean =
                oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position).role) {
            ChatRole.USER -> VIEW_USER
            ChatRole.ASSISTANT -> VIEW_ASSISTANT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_USER -> UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
            else -> AssistantViewHolder(ItemChatAssistantBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = getItem(position)
        when (holder) {
            is UserViewHolder -> holder.bind(msg)
            is AssistantViewHolder -> {
                val showAvatar = position == 0 || getItem(position - 1).role != ChatRole.ASSISTANT
                holder.bind(msg, showAvatar)
            }
        }
    }

    class UserViewHolder(
        private val binding: ItemChatUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: ChatMessage) {
            binding.tvUserMessage.text = msg.text
        }
    }

    inner class AssistantViewHolder(
        private val binding: ItemChatAssistantBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var dotAnimators: List<ObjectAnimator> = emptyList()

        fun bind(msg: ChatMessage, showAvatar: Boolean) {
            binding.assistantAvatar.visibility = if (showAvatar) View.VISIBLE else View.INVISIBLE

            val showDots = msg.isStreaming && msg.text.isBlank()
            binding.typingDots.isVisible = showDots
            binding.tvAssistantMessage.isVisible = !showDots
            binding.tvAssistantMessage.text = msg.text

            if (showDots) {
                startDotAnimations()
            } else {
                stopDotAnimations()
            }

            if (msg.sources.isNotEmpty() && !msg.isStreaming) {
                binding.sourcesScroll.isVisible = true
                binding.sourcesContainer.removeAllViews()
                val inflater = LayoutInflater.from(binding.root.context)
                msg.sources.forEach { src ->
                    val chip = ItemSourceChipBinding.inflate(inflater, binding.sourcesContainer, false)
                    chip.tvSourceLabel.text = binding.root.context.getString(
                        R.string.chat_source_chip_fmt,
                        src.title,
                        src.page
                    )
                    chip.root.setOnClickListener { onSourceClick(src) }
                    binding.sourcesContainer.addView(chip.root)
                }
            } else {
                binding.sourcesScroll.isVisible = false
            }
        }

        private fun startDotAnimations() {
            if (dotAnimators.isNotEmpty()) return
            val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
            dotAnimators = dots.mapIndexed { index, dot ->
                ObjectAnimator.ofFloat(dot, View.TRANSLATION_Y, 0f, -8f, 0f).apply {
                    duration = 650L
                    startDelay = (index * 130L)
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
            }
        }

        private fun stopDotAnimations() {
            dotAnimators.forEach { it.cancel() }
            dotAnimators = emptyList()
            binding.dot1.translationY = 0f
            binding.dot2.translationY = 0f
            binding.dot3.translationY = 0f
        }
    }
}

