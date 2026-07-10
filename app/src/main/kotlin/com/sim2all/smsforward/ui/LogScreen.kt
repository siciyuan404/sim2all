package com.sim2all.smsforward.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sim2all.smsforward.data.PendingSms
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LogScreen(
    vm: AppViewModel,
    contentPadding: PaddingValues
) {
    val list by vm.recent.collectAsStateWithLifecycle()

    if (list.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "暂无记录",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "收到短信后将在此显示",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        items(list, key = { it.id }) { item ->
            LogItem(item)
            HorizontalDivider()
        }
    }
}

@Composable
private fun LogItem(item: PendingSms) {
    val df = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.sender,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                StatusChip(item.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!item.lastError.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "错误：${item.lastError}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    append("收到：").append(df.format(Date(item.receivedAt)))
                    if (item.sentAt != null) {
                        append("  发送：").append(df.format(Date(item.sentAt)))
                    }
                    if (item.retryCount > 0) {
                        append("  重试：").append(item.retryCount)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (text, color) = when (status) {
        PendingSms.STATUS_SENT -> "已发送" to MaterialTheme.colorScheme.primary
        PendingSms.STATUS_FAILED -> "失败" to MaterialTheme.colorScheme.error
        PendingSms.STATUS_SENDING -> "发送中" to MaterialTheme.colorScheme.tertiary
        else -> "待发送" to MaterialTheme.colorScheme.secondary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
