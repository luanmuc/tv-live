package com.doubao.tv.ui.screen.settings.subcategories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Switch
import androidx.tv.material3.Text

/**
 * 本机新增直播源/节目单：HTTP 推送服务退场后，用遥控器或外接键盘直接填写。
 */
@Composable
fun AddSourceDialog(
    title: String,
    nameHint: String,
    urlHint: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, allowSelfSignedTls: Boolean) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var allowSelfSignedTls by remember { mutableStateOf(false) }
    val canConfirm = url.isNotBlank()

    Column(
        modifier = Modifier
            .width(560.dp)
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        SourceTextField(value = name, onValueChange = { name = it }, hint = nameHint)
        SourceTextField(value = url, onValueChange = { url = it }, hint = urlHint)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Switch(checked = allowSelfSignedTls, onCheckedChange = { allowSelfSignedTls = it })
            Text("允许自签名 HTTPS 证书（仅此源）", fontSize = 14.sp)
        }
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onDismiss) { Text("取消") }
            Button(
                onClick = {
                    if (canConfirm) {
                        onConfirm(name.ifBlank { "自定义源" }, url.trim(), allowSelfSignedTls)
                    }
                },
            ) { Text("添加") }
        }
    }
}

@Composable
private fun SourceTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(text = hint, color = Color(0xFF9E9E9E), fontSize = 16.sp)
            }
            innerTextField()
        },
    )
}
