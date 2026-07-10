package com.sim2all.smsforward.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sim2all.smsforward.data.Settings

@Composable
fun ConfigScreen(
    vm: AppViewModel,
    onOpenLog: () -> Unit,
    contentPadding: PaddingValues
) {
    val snapNullable by vm.snapshot.collectAsStateWithLifecycle()
    val testState by vm.testState.collectAsStateWithLifecycle()
    val pendingCount by vm.pendingCount.collectAsStateWithLifecycle()
    val snap = snapNullable ?: return

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ===== 总开关 =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "短信转发",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (snap.enabled) "已开启，将自动转发短信" else "已关闭",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Switch(
                    checked = snap.enabled,
                    onCheckedChange = { v -> vm.update { it.copy(enabled = v) } }
                )
            }
        }

        // ===== SMTP 服务器 =====
        SectionCard(title = "SMTP 服务器", icon = Icons.Outlined.MarkEmailRead) {
            OutlinedTextField(
                value = snap.smtpHost,
                onValueChange = { v -> vm.update { it.copy(smtpHost = v.trim()) } },
                label = { Text("SMTP 主机") },
                placeholder = { Text("smtp.qq.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = snap.smtpPort.toString(),
                    onValueChange = { v ->
                        val p = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                        vm.update { it.copy(smtpPort = p) }
                    },
                    label = { Text("端口") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = snap.useSsl,
                            onCheckedChange = { v -> vm.update { it.copy(useSsl = v, useStartTls = if (v) false else it.useStartTls) } }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("SSL", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = snap.useStartTls,
                            onCheckedChange = { v -> vm.update { it.copy(useStartTls = v, useSsl = if (v) false else it.useSsl) } }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("STARTTLS", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedTextField(
                value = snap.smtpUser,
                onValueChange = { v -> vm.update { it.copy(smtpUser = v.trim()) } },
                label = { Text("登录账号") },
                placeholder = { Text("user@qq.com") },
                leadingIcon = { Icon(Icons.Outlined.AlternateEmail, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = snap.smtpPass,
                onValueChange = { v -> vm.update { it.copy(smtpPass = v) } },
                label = { Text("授权码 / 密码") },
                placeholder = { Text("QQ邮箱请使用授权码而非登录密码") },
                leadingIcon = { Icon(Icons.Outlined.Password, null) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ===== 收发邮箱 =====
        SectionCard(title = "收发邮箱", icon = Icons.Outlined.Send) {
            OutlinedTextField(
                value = snap.fromAddress,
                onValueChange = { v -> vm.update { it.copy(fromAddress = v.trim()) } },
                label = { Text("发件邮箱（留空则用登录账号）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = snap.toAddress,
                onValueChange = { v -> vm.update { it.copy(toAddress = v) } },
                label = { Text("收件邮箱（多个用英文逗号分隔）") },
                leadingIcon = { Icon(Icons.Outlined.MarkEmailRead, null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
            )
            OutlinedTextField(
                value = snap.subjectPrefix,
                onValueChange = { v -> vm.update { it.copy(subjectPrefix = v) } },
                label = { Text("邮件主题前缀") },
                leadingIcon = { Icon(Icons.Outlined.Tag, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = snap.signature,
                onValueChange = { v -> vm.update { it.copy(signature = v) } },
                label = { Text("邮件签名（可空）") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
            )
        }

        // ===== 过滤规则 =====
        SectionCard(title = "过滤规则", icon = Icons.Outlined.FilterAlt) {
            Text(
                text = "两个条件都留空表示转发全部短信；填了则只转发满足条件的短信。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = snap.senderFilter,
                onValueChange = { v -> vm.update { it.copy(senderFilter = v) } },
                label = { Text("发送方号码过滤（逗号分隔）") },
                leadingIcon = { Icon(Icons.Outlined.Key, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = snap.keywordFilter,
                onValueChange = { v -> vm.update { it.copy(keywordFilter = v) } },
                label = { Text("正文关键字过滤（逗号分隔）") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("最大重试次数", Modifier.weight(1f))
                OutlinedButton(onClick = {
                    vm.update { it.copy(maxRetry = (it.maxRetry - 1).coerceAtLeast(0)) }
                }) { Text("-") }
                Spacer(Modifier.width(8.dp))
                Text("${snap.maxRetry}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    vm.update { it.copy(maxRetry = it.maxRetry + 1) }
                }) { Text("+") }
            }
        }

        // ===== 操作 =====
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { vm.sendTest() },
                modifier = Modifier.weight(1f)
            ) { Text("发送测试邮件") }
            OutlinedButton(
                onClick = onOpenLog,
                modifier = Modifier.weight(1f)
            ) { Text("日志 ($pendingCount)") }
        }

        when (val st = testState) {
            AppViewModel.TestState.Running ->
                Text("发送中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            AppViewModel.TestState.Success ->
                Text("测试邮件已发送，请到收件箱确认。",
                    color = MaterialTheme.colorScheme.primary)
            is AppViewModel.TestState.Failed -> Column {
                Text("发送失败：${st.message}", color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { vm.clearTestState() }) { Text("知道了") }
            }
            AppViewModel.TestState.Idle -> Unit
        }

        HorizontalDivider()
        Text(
            text = "提示：QQ邮箱请使用授权码；Gmail 请开启两步验证后使用应用专用密码。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
