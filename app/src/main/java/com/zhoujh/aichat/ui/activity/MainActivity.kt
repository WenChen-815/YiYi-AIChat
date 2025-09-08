package com.zhoujh.aichat.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.zhoujh.aichat.databinding.ActivityMainBinding
import com.zhoujh.aichat.app.manager.AIChatManager
import com.zhoujh.aichat.app.manager.ConfigManager
import com.zhoujh.aichat.ui.fragment.HomeFragment
import com.zhoujh.aichat.ui.fragment.ProfileFragment
import com.zhoujh.aichat.utils.StatusBarUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var configManager: ConfigManager
    private val fragmentList = listOf(
        HomeFragment(),
//        PlaceholderFragment(), // 占位Fragment，对应中间按钮位置
        ProfileFragment()
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        StatusBarUtil.transparentNavBar(this)
        // 设置状态栏文字颜色为白色
        StatusBarUtil.setStatusBarTextColor(this, false)

        // 初始化配置管理器
        configManager = ConfigManager()
        // 检查是否有配置，如果没有则跳转到配置页面
        if (!configManager.hasCompleteConfig()) {
            startActivity(Intent(this, ConfigActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AIChatManager.init()

        // 初始化ViewPager2
        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragmentList.size
            override fun createFragment(position: Int) = fragmentList[position]
        }

        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false // 禁用滑动切换
        // 底部导航栏点击事件
        initNavClick()
    }

    private fun initNavClick() {
        // 首页点击
        binding.navHome.setOnClickListener {
            switchPage(0)
        }

        // 搜索点击
//        binding.navSearch.setOnClickListener {
//            switchPage(1)
//        }

        // 发布按钮点击（单独逻辑，不切换页面）
        binding.navAdd.setOnClickListener {
            // 打开发布页面的逻辑（如启动新Activity）
            startActivity(Intent(this, CharacterEditActivity::class.java))
        }

        // 消息点击
//        binding.navMessage.setOnClickListener {
//            switchPage(3)
//        }

        // 我的点击
        binding.navMine.setOnClickListener {
            switchPage(2)
        }

        // 默认选中首页
        binding.navHome.isSelected = true
    }

    // 切换页面并更新导航项选中状态
    private fun switchPage(position: Int) {
        // 更新ViewPager2页面
        binding.viewPager.currentItem = position

        // 重置所有导航项的选中状态
        binding.navHome.isSelected = false
//        binding.navSearch.isSelected = false
//        binding.navMessage.isSelected = false
        binding.navMine.isSelected = false

        // 设置当前导航项为选中状态
        when (position) {
            0 -> binding.navHome.isSelected = true
//            1 -> binding.navSearch.isSelected = true
//            3 -> binding.navMessage.isSelected = true
            2 -> binding.navMine.isSelected = true
        }
    }
}

