package com.zhoujh.aichat.ui.adapter

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zhoujh.aichat.databinding.ItemAiMessageBinding
import com.zhoujh.aichat.databinding.ItemUserMessageBinding
import com.zhoujh.aichat.database.entity.ChatMessage
import com.zhoujh.aichat.database.entity.MessageContentType
import com.zhoujh.aichat.database.entity.MessageType
import com.zhoujh.aichat.utils.ChatUtil
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.zhoujh.aichat.R
import com.zhoujh.aichat.ui.activity.ImgTestActivity

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatMessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
        private const val VIEW_TYPE_SYSTEM = 3
    }

    // 回调接口
    var onImageLoadedCallback: (() -> Unit)? = null

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
        /*
         * [修复 图片消息布局错乱问题]
         * > 错因:RecycleView高效的复用机制，让ViewHolder的控件自动复用在本次布局中 被复用控件有设置 但当前控件未设置的属性
         * 例如:bind方法中通过判断资源类型设置图片资源/文本资源，现在子控件a调用了ImageView设置了图片资源，
         * 而子控件b仅调用TextView设置文本资源，如果滚动到子控件b且恰好复用子控件a时，
         * 由于子控件b的ImageView没有设置过图片资源，所以复用子控件a的图片展示，
         * 又因自身设置了文本资源，导致两种资源叠加展示。蒸的饭！*/
        fun bind(message: ChatMessage) {
            when(message.contentType) {
                MessageContentType.TEXT -> {
                    binding.img.visibility = View.GONE
                    binding.tvUserMessage.visibility = View.VISIBLE
                    binding.tvUserMessage.text = message.content
                    binding.bg.background = AppCompatResources.getDrawable(binding.root.context,R.drawable.bubble_me)
                }
                MessageContentType.IMAGE -> {
                    binding.img.visibility = View.VISIBLE
                    binding.tvUserMessage.visibility = View.GONE
                    showImage(message,binding.bg,binding.root,binding.img)
                }
//                MessageContentType.VOICE -> binding.ivUserMessage.setImageURI(message.voiceUrl)
                else -> {}
            }
        }
    }

    inner class AiMessageViewHolder(private val binding: ItemAiMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            when(message.contentType) {
                MessageContentType.TEXT -> {
                    binding.img.visibility = View.GONE
                    binding.tvAiMessage.visibility = View.VISIBLE
                    binding.tvAiMessage.text = message.content
                    binding.bg.background = AppCompatResources.getDrawable(binding.root.context,R.drawable.bubble_other)
                }
                MessageContentType.IMAGE -> {
                    binding.img.visibility = View.VISIBLE
                    binding.tvAiMessage.visibility = View.GONE
                    showImage(message,binding.bg,binding.root,binding.img)
                }
//                MessageContentType.VOICE -> binding.ivAiMessage.setImageURI(message.voiceUrl)
                else -> {}
            }
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
            // 解析AI回复中的日期和角色名称
            var cleanedContent = ChatUtil.parseMessage(message)
            if (message.type == MessageType.AI && cleanedContent.contains('\\') && message.contentType != MessageContentType.IMAGE && message.contentType != MessageContentType.VOICE) {
                // 分割AI消息内容
                val parts = cleanedContent.split('\\').filter { it.isNotBlank() }

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
                            contentType = message.contentType,
                            characterId = message.characterId,
                            chatUserId = message.chatUserId
                        )
                    )
                }
            } else {
                // 非AI消息或不包含\的AI消息直接添加
                processedMessagesTemp.add(ChatMessage(
                    id = message.id,
                    content = cleanedContent,
                    type = message.type,
                    timestamp = message.timestamp,
                    characterId = message.characterId,
                    chatUserId = message.chatUserId,
                    contentType = message.contentType,
                    imgUrl = message.imgUrl,
                    voiceUrl = message.voiceUrl,
                    isShow = message.isShow
                ))
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

    private fun showImage(message: ChatMessage,bg: RelativeLayout,root: LinearLayout, img: ImageView) {
        bg.background = null
        // 使用Glide加载图片
        Glide.with(root)
            .load(message.imgUrl?.toUri())
            .fitCenter()
            .override(200, 200) // 设置最大尺寸
            // 添加圆角效果，参数为圆角半径（单位：像素）
            .transform(RoundedCorners(10)) // 10像素的圆角
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
//                    Log.e("ChatAdapter", "Image load failed: ${e?.message}")
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
//                    Log.d("ChatAdapter", "Image loaded successfully")
                    return false
                }
            })
            .into(img)
    }
}