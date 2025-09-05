package com.zhoujh.aichat.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zhoujh.aichat.databinding.ItemAiMessageBinding
import com.zhoujh.aichat.databinding.ItemUserMessageBinding
import com.zhoujh.aichat.database.entity.ChatMessage
import com.zhoujh.aichat.database.entity.MessageType

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    // 存储处理后的消息列表
    private var processedMessages: List<ChatMessage> = emptyList()

    // 获取处理后的消息列表大小
    fun getProcessedItemCount(): Int {
        return processedMessages.size
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position).type) {
            MessageType.USER -> VIEW_TYPE_USER
            MessageType.AI -> VIEW_TYPE_AI
            MessageType.SYSTEM -> VIEW_TYPE_SYSTEM
            else -> throw IllegalArgumentException("未知的消息类型")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemUserMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            UserMessageViewHolder(binding)
        } else {
            val binding = ItemAiMessageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            AiMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when(holder) {
            is UserMessageViewHolder -> holder.bind(message)
            is AiMessageViewHolder -> holder.bind(message)
        }
    }

    inner class UserMessageViewHolder(private val binding: ItemUserMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvUserMessage.text = message.content
        }
    }

    inner class AiMessageViewHolder(private val binding: ItemAiMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvAiMessage.text = message.content
        }
    }

    class ChatMessageDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }

    fun setMessages(messages: List<ChatMessage>, callback: (() -> Unit)? = null) {
        val processedMessagesTemp = mutableListOf<ChatMessage>()

        for (message in messages) {
            if (message.type == MessageType.AI && message.content.contains('\\')) {
                // 分割AI消息内容
                val parts = message.content.split('\\').filter { it.isNotBlank() }

                // 为每个部分创建一个新的ChatMessage对象
                for (i in parts.indices) {
                    // 确保每个消息ID唯一
                    val newId = message.id + "_part_" + i
                    // 创建新的消息对象，保持原始消息的大部分属性
                    processedMessagesTemp.add(
                        ChatMessage(
                            id = newId,
                            content = parts[i],
                            type = message.type,
                            timestamp = message.timestamp + i, // 稍微调整时间戳以保证唯一性
                            characterId = message.characterId,
                            chatUserId = message.chatUserId
                        )
                    )
                }
            } else {
                // 非AI消息或不包含\的AI消息直接添加
                processedMessagesTemp.add(message)
            }
        }
        // 存储处理后的消息
        processedMessages = processedMessagesTemp
        // 设置处理后的消息列表，并传递回调
        submitList(processedMessages, callback)
    }

    // 添加更多消息到列表
    fun addMoreMessages(newMessages: List<ChatMessage>, callback: (() -> Unit)? = null) {
        val currentList = currentList.toMutableList()
        currentList.addAll(0,newMessages)
        setMessages(currentList, callback)
    }
}