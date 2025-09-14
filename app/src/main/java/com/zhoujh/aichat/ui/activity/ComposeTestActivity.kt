package com.zhoujh.aichat.ui.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zhoujh.aichat.R

class ComposeTestActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HelloCompose()
        }
    }
}
@Preview
@Composable
fun HelloCompose() {
    Column {
        Text(
            text = "Hello Compose",
            fontSize = 14.sp,
            color = Color.Red,
            fontWeight = FontWeight.Bold
        )
        var count = 0
        var context = LocalContext.current
        Button(
            onClick = {
                count++
                Toast.makeText(context, "Click me $count", Toast.LENGTH_SHORT).show()
            }) {
            Text(
                text = "Click me $count",
                color = Color.Blue
            )
        }
        TextField(
            value = "",
            label = {
                Text("请输入")
            },
            placeholder = {
                Text("holder")
            },
            onValueChange = {
                count = it.toInt()
            },
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Blue,
                unfocusedIndicatorColor = Color.Gray
            )
        )
//        val bitmap: ImageBitmap = ImageBitmap.imageResource(id = R.drawable.dog)
//        Image(
//            bitmap = bitmap,
//            contentDescription = "A dog image"
//        )
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(100.dp)
        )
        // implementation("io.coil-kt:coil-compose:2.4.0")
        // 下面这个可以加载网络图片，但需添加依赖
//        AsyncImage(
//            model = "https://img-blog.csdnimg.cn/20200401094829557.jpg",
//            contentDescription = "First line of code"
//        )
    }
}
