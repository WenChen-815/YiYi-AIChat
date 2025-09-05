package com.zhoujh.aichat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.zhoujh.aichat.R
import com.zhoujh.aichat.model.AICharacter
import java.text.SimpleDateFormat
import java.util.Locale

class CharacterAdapter(
    private val onCharacterClick: (AICharacter) -> Unit,
    private val onEditClick: (AICharacter) -> Unit,
    private val onDeleteClick: (AICharacter) -> Unit
) : PagingDataAdapter<AICharacter, CharacterAdapter.CharacterViewHolder>(CharacterDiffCallback()) {

    inner class CharacterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.tv_character_name)
        private val promptPreviewTextView: TextView = itemView.findViewById(R.id.tv_prompt_preview)
        private val dateTextView: TextView = itemView.findViewById(R.id.tv_create_time)
        private val editButton: Button = itemView.findViewById(R.id.btn_edit)
        private val deleteButton: Button = itemView.findViewById(R.id.btn_delete)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(character: AICharacter) {
            nameTextView.text = character.name
            promptPreviewTextView.text = if (character.prompt.length > 50) {
                "${character.prompt.substring(0, 50)}..."
            } else {
                character.prompt
            }
            dateTextView.text = dateFormat.format(character.createdAt)

            // 点击整个项
            itemView.setOnClickListener {
                onCharacterClick(character)
            }

            // 编辑按钮
            editButton.setOnClickListener {
                onEditClick(character)
            }

            // 删除按钮
            deleteButton.setOnClickListener {
                onDeleteClick(character)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_character, parent, false)
        return CharacterViewHolder(view)
    }

    override fun onBindViewHolder(holder: CharacterViewHolder, position: Int) {
        holder.bind(getItem(position)!!)
    }

    class CharacterDiffCallback : DiffUtil.ItemCallback<AICharacter>() {
        override fun areItemsTheSame(oldItem: AICharacter, newItem: AICharacter): Boolean {
            return oldItem.aiCharacterId == newItem.aiCharacterId
        }

        override fun areContentsTheSame(oldItem: AICharacter, newItem: AICharacter): Boolean {
            return oldItem == newItem
        }
    }
}