package com.zhoujh.aichat.ui.activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    private val REQUEST_CODE_PERMISSION = 1002
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

        // 在进入界面时动态请求权限
        requestImagePermissionOnStartup()

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
    // 在进入界面时请求权限的方法
    private fun requestImagePermissionOnStartup() {
        // 检查Android版本，适配不同的权限模型
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14及以上版本 - 使用新的精选照片API，无需提前请求权限
            Log.d("ImgTestActivity", "Android 14+，使用精选照片API无需提前请求权限")
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 (API 33) - 使用READ_MEDIA_IMAGES权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    REQUEST_CODE_PERMISSION
                )
            }
        } else {
            // Android 12及以下版本 - 使用READ_EXTERNAL_STORAGE权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    REQUEST_CODE_PERMISSION
                )
            }
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("ImgTestActivity", "图片访问权限已授予")
            } else {
                // 权限被拒绝，显示提示信息
                Toast.makeText(this, "需要存储权限才能选择图片", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

