package com.sim2all.smsforward.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sim2all.smsforward.App
import com.sim2all.smsforward.data.PendingSms
import com.sim2all.smsforward.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 配置与日志共用 ViewModel。
 *
 * - snapshot：当前配置的可编辑镜像
 * - recent：最近 200 条短信记录
 */
class AppViewModel : ViewModel() {

    private val app get() = App.instance

    val snapshot: StateFlow<Settings.Snapshot?> = app.settings.data
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val recent: StateFlow<List<PendingSms>> = app.db.pendingSmsDao()
        .observeRecent(200)
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val pendingCount: StateFlow<Int> = app.db.pendingSmsDao()
        .observePendingCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    fun update(transform: (Settings.Snapshot) -> Settings.Snapshot) {
        viewModelScope.launch {
            app.settings.update(transform)
        }
    }

    /** 发送测试邮件 */
    fun sendTest() {
        viewModelScope.launch {
            _testState.value = TestState.Running
            try {
                val cfg = app.settings.snapshot()
                if (cfg.recipients.isEmpty()) {
                    _testState.value = TestState.Failed("收件邮箱为空")
                    return@launch
                }
                val sender = com.sim2all.smsforward.mail.MailSender(cfg)
                sender.send(
                    sender = "测试",
                    body = "这是一封来自 sim2all 的测试邮件，配置生效。",
                    receivedAt = java.util.Date()
                )
                _testState.value = TestState.Success
            } catch (t: Throwable) {
                _testState.value = TestState.Failed(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    fun clearTestState() { _testState.value = TestState.Idle }

    sealed class TestState {
        data object Idle : TestState()
        data object Running : TestState()
        data object Success : TestState()
        data class Failed(val message: String) : TestState()
    }
}
