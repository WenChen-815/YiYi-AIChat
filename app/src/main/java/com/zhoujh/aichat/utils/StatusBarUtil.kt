package com.zhoujh.aichat.utils

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class StatusBarUtil {
    companion object {
        fun transparentNavBar(activity: Activity) {
            val window = activity.window
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (window.attributes.flags and WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION == 0) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                }
            }

            val decorView = window.decorView
            val vis = decorView.systemUiVisibility
            val option = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = vis or option
        }

        fun transparentStatusBar(window: Window) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            var systemUiVisibility = window.decorView.systemUiVisibility
            systemUiVisibility =
                systemUiVisibility or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            window.decorView.systemUiVisibility = systemUiVisibility
            window.statusBarColor = android.graphics.Color.TRANSPARENT

        }

        /**
         * 设置状态栏文字颜色（深色/浅色）,注意：如果你使用了compose的方法设置了状态栏(如enableEdgeToEdge）此方法将失效
         * @param activity 当前活动
         * @param dark 是否设置为深色文字
         */
        fun setAndroidNativeLightStatusBar(activity: Activity, dark: Boolean) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val decor: View = activity.window.decorView
                    decor.systemUiVisibility = if (dark) {
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    } else {
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    }
                }
            } catch (ignore: Exception) {
                // 忽略异常
            }
        }

        fun getStatusBarHeight(context: Context): Int {
            val resId = context.resources.getIdentifier(
                "status_bar_height", "dimen", "android"
            )
            return context.resources.getDimensionPixelSize(resId)
        }

        fun fixStatusBarMargin(vararg views: View) {
            views.forEach { view ->
                (view.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                    lp.topMargin = lp.topMargin + getStatusBarHeight(view.context)
                    view.requestLayout()
                }
            }
        }

        fun paddingByStatusBar(view: View) {
            view.setPadding(
                view.paddingLeft,
                view.paddingTop + getStatusBarHeight(view.context),
                view.paddingRight,
                view.paddingBottom
            )
        }

        /**
         * 仅当view attach window后生效
         */
        private fun getRealNavigationBarHeight(view: View): Int {
            val insets = ViewCompat.getRootWindowInsets(view)
                ?.getInsets(WindowInsetsCompat.Type.navigationBars())
            //WindowInsets为null则默认通过资源获取高度
            return insets?.bottom ?: getNavigationBarHeight(view.context)
        }
        /**
         * 通过系统资源获取导航栏高度
         * @param context 上下文对象
         * @return 导航栏高度，若获取失败则返回0
         */
        fun getNavigationBarHeight(context: Context): Int {
            // 获取系统定义的导航栏高度资源ID
            val resId = context.resources.getIdentifier(
                "navigation_bar_height", "dimen", "android"
            )
            // 检查资源ID是否有效
            return if (resId > 0) {
                try {
                    // 获取并返回导航栏高度（像素）
                    context.resources.getDimensionPixelSize(resId)
                } catch (e: Exception) {
                    // 处理可能的异常（如资源未找到）
                    0
                }
            } else {
                // 资源ID无效时返回0
                0
            }
        }

        // ===========Compose相关===========
        /**
         * 在Activity中直接设置状态栏文字颜色
         * @param activity 当前Activity
         * @param isDarkText 是否设置为深色文字（true: 深色文字，适合浅色背景; false: 浅色文字，适合深色背景）
         * @param statusBarColor 状态栏背景颜色，默认透明
         */
        fun setStatusBarTextColor(
            activity: ComponentActivity,
            isDarkText: Boolean,
            statusBarColor: Color = Color.Transparent
        ) {
            val window = activity.window
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

            // 设置状态栏文字颜色模式
            windowInsetsController.isAppearanceLightStatusBars = isDarkText

            // 设置状态栏背景色
            window.statusBarColor = statusBarColor.toArgb()
        }

        /**
         * 在Compose组件中设置状态栏文字颜色
         * @param isDarkText 是否设置为深色文字
         * @param statusBarColor 状态栏背景颜色
         */
        @Composable
        fun ConfigureStatusBar(
            isDarkText: Boolean,
            statusBarColor: Color = Color.Black
        ) {
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    val windowInsetsController = WindowCompat.getInsetsController(window, view)

                    // 设置状态栏文字颜色模式
                    windowInsetsController.isAppearanceLightStatusBars = isDarkText

                    // 设置状态栏背景色
                    window.statusBarColor = statusBarColor.toArgb()
                }
            }
        }
    }
}