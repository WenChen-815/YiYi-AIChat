package com.zhoujh.aichat.ui.fragment

import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.zhoujh.aichat.app.AppContext
import com.zhoujh.aichat.ui.adapter.CharacterAdapter
import com.zhoujh.aichat.database.dao.AICharacterDao
import com.zhoujh.aichat.databinding.FragmentHomeBinding
import com.zhoujh.aichat.ui.activity.CharacterEditActivity
import com.zhoujh.aichat.ui.viewmodel.CharacterViewModel
import com.zhoujh.aichat.ui.activity.ChatActivity
import com.zhoujh.aichat.app.manager.ConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: CharacterViewModel
    private lateinit var characterAdapter: CharacterAdapter
    private lateinit var aiCharacterDao: AICharacterDao

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.btnChat.setOnClickListener {
            val intent = Intent(requireContext(), ChatActivity::class.java)
            startActivity(intent)
        }

        aiCharacterDao = AppContext.appDatabase.aiCharacterDao()

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[CharacterViewModel::class.java]

        // 初始化适配器
        characterAdapter = CharacterAdapter(
            onCharacterClick = { character ->
                Log.d("CharacterListActivity", "选择角色：${character.name}")
                ConfigManager().saveSelectedCharacterId(character.aiCharacterId)
                val intent = Intent(requireContext(), ChatActivity::class.java)
                intent.putExtra("SELECTED_CHARACTER", Gson().toJson(character))
                startActivity(intent)
            },
            onEditClick = { character ->
                // 编辑角色
                val intent = Intent(activity, CharacterEditActivity::class.java)
                intent.putExtra("CHARACTER_ID", character.aiCharacterId)
                startActivity(intent)
            },
            onDeleteClick = { character ->
                lifecycleScope.launch(Dispatchers.IO) {
                    // 删除角色
                    val result = aiCharacterDao.deleteAICharacter(character)
                    withContext(Dispatchers.Main){
                        if (result > 0) {
                            Toast.makeText(activity, "删除成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "删除失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        // 设置RecyclerView
        binding.rvCharacters.apply {
            layoutManager = LinearLayoutManager(activity)
            adapter = characterAdapter
        }

        // 设置对 viewModel.loadData 的观察者，当数据发生变化时，调用 loadData 方法
        viewModel.loadData.observe(activity){ data-> loadData() }

        loadData()

        binding.search.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                // 隐藏键盘
//                val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                val imm = (activity?.getSystemService(INPUT_METHOD_SERVICE) ?: return@OnEditorActionListener false) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.search.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

                // 用户完成输入，比如按下了回车键或点击了完成按钮
                val inputText: String = binding.search.text.toString()
                viewModel.setQuery(inputText)

                return@OnEditorActionListener true // 表示事件已处理
            }
            false
        })

        return binding.root
    }

    private fun loadData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.aiCharacters()
                    .catch { e -> Log.e("FlowError", "Error: $e") }
                    .collectLatest { characterAdapter.submitData(it) }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}