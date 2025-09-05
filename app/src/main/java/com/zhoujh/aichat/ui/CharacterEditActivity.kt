package com.zhoujh.aichat.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.zhoujh.aichat.AppContext
import com.zhoujh.aichat.database.AICharacterDao
import com.zhoujh.aichat.databinding.ActivityCharacterEditBinding
import com.zhoujh.aichat.model.AICharacter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class CharacterEditActivity : AppCompatActivity(), CoroutineScope by MainScope(){
    private lateinit var binding: ActivityCharacterEditBinding
    private lateinit var viewModel: CharacterViewModel
    private var characterId: String? = null
    private lateinit var aiCharacterDao: AICharacterDao
    private val userId = AppContext.USER_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharacterEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        aiCharacterDao = AppContext.appDatabase.aiCharacterDao()

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[CharacterViewModel::class.java]

        // 检查是否是编辑模式
        intent.getStringExtra("CHARACTER_ID")?.let { id ->
            characterId = id
            loadCharacterData(characterId!!)
        }

        // 保存按钮点击事件
        binding.btnSave.setOnClickListener {
            saveCharacter()
        }

        // 取消按钮点击事件
        binding.btnCancel.setOnClickListener {
            finish()
        }

        // 选择头像按钮
        binding.btnAvatarPath.setOnClickListener {
            // 这里可以实现选择头像的逻辑
            Toast.makeText(this, "选择头像功能待实现", Toast.LENGTH_SHORT).show()
        }

        // 选择背景按钮
        binding.btnBackgroundPath.setOnClickListener {
            // 这里可以实现选择背景的逻辑
            Toast.makeText(this, "选择背景功能待实现", Toast.LENGTH_SHORT).show()
        }
    }

    // 加载角色数据
    private fun loadCharacterData(characterId: String) {
        launch(Dispatchers.IO){
            var character = aiCharacterDao.getCharacterById(characterId)
            withContext(Dispatchers.Main) {
                binding.etCharacterName.setText(character?.name)
                binding.etPrompt.setText(character?.prompt)
                // 如果有头像或背景图，可以在这里设置预览
            }
        }
    }

    // 保存角色数据
    private fun saveCharacter() {
        val name = binding.etCharacterName.text.toString().trim()
        val prompt = binding.etPrompt.text.toString().trim()

        if (name.isEmpty()) {
            binding.etCharacterName.error = "请输入角色名称"
            return
        }

        if (prompt.isEmpty()) {
            binding.etPrompt.error = "请输入提示词"
            return
        }

        val character = AICharacter(
            aiCharacterId = characterId ?: UUID.randomUUID().toString(),
            name = name,
            prompt = prompt,
            userId = userId,
            createdAt = System.currentTimeMillis(),
            avatarPath = null, // 实际应用中可以设置为选择的图片路径
            backgroundPath = null // 实际应用中可以设置为选择的图片路径
        )

        if (characterId == null) {
            launch(Dispatchers.IO) {
                var result = aiCharacterDao.insertAICharacter(character)
                withContext(Dispatchers.Main){
                    if (result > 0) {
                        Toast.makeText(this@CharacterEditActivity, "添加成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CharacterEditActivity, "添加失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            launch(Dispatchers.IO) {
                var result = aiCharacterDao.updateAICharacter(character)
                withContext(Dispatchers.Main){
                    if (result > 0) {
                        Toast.makeText(this@CharacterEditActivity, "更新成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@CharacterEditActivity, "更新失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        finish()
    }
}