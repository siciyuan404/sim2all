package com.sim2all.smsforward.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 用户配置。基于 DataStore Preferences 持久化。
 *
 * 字段：
 * - smtpHost / smtpPort / smtpUser / smtpPass / useStartTls / useSsl
 * - fromAddress（发件邮箱，缺省与 smtpUser 一致）
 * - toAddress（收件邮箱，多邮箱以英文逗号分隔）
 * - senderFilter（仅转发这些发送方号码，留空表示全部；英文逗号分隔）
 * - keywordFilter（仅转发包含这些关键字的短信，留空表示全部；英文逗号分隔）
 * - mailSubjectPrefix（邮件主题前缀）
 * - enabled（总开关）
 * - maxRetry（最大重试次数，默认 5）
 */
private val Context.dataStore by preferencesDataStore(name = "sms_forward_settings")

class Settings(private val context: Context) {

    object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val SMTP_HOST = stringPreferencesKey("smtp_host")
        val SMTP_PORT = intPreferencesKey("smtp_port")
        val SMTP_USER = stringPreferencesKey("smtp_user")
        val SMTP_PASS = stringPreferencesKey("smtp_pass")
        val USE_STARTTLS = booleanPreferencesKey("use_starttls")
        val USE_SSL = booleanPreferencesKey("use_ssl")
        val FROM_ADDRESS = stringPreferencesKey("from_address")
        val TO_ADDRESS = stringPreferencesKey("to_address")
        val SENDER_FILTER = stringPreferencesKey("sender_filter")
        val KEYWORD_FILTER = stringPreferencesKey("keyword_filter")
        val SUBJECT_PREFIX = stringPreferencesKey("subject_prefix")
        val MAX_RETRY = intPreferencesKey("max_retry")
        val SIGNATURE = stringPreferencesKey("signature")
    }

    val data: Flow<Snapshot> = context.dataStore.data.map { p ->
        val user = p[Keys.SMTP_USER].orEmpty()
        Snapshot(
            enabled = p[Keys.ENABLED] ?: false,
            smtpHost = p[Keys.SMTP_HOST] ?: "smtp.qq.com",
            smtpPort = p[Keys.SMTP_PORT] ?: 465,
            smtpUser = user,
            smtpPass = p[Keys.SMTP_PASS] ?: "",
            useStartTls = p[Keys.USE_STARTTLS] ?: false,
            useSsl = p[Keys.USE_SSL] ?: true,
            fromAddress = p[Keys.FROM_ADDRESS]?.ifEmpty { user }.orEmpty(),
            toAddress = p[Keys.TO_ADDRESS] ?: "",
            senderFilter = p[Keys.SENDER_FILTER] ?: "",
            keywordFilter = p[Keys.KEYWORD_FILTER] ?: "",
            subjectPrefix = p[Keys.SUBJECT_PREFIX] ?: "[短信转发]",
            maxRetry = p[Keys.MAX_RETRY] ?: 5,
            signature = p[Keys.SIGNATURE] ?: ""
        )
    }

    /** 读取一次性快照（适合在 Service/Worker 中使用） */
    suspend fun snapshot(): Snapshot = data.first()

    suspend fun update(block: (Snapshot) -> Snapshot) {
        context.dataStore.edit { p ->
            val current = Snapshot(
                enabled = p[Keys.ENABLED] ?: false,
                smtpHost = p[Keys.SMTP_HOST] ?: "smtp.qq.com",
                smtpPort = p[Keys.SMTP_PORT] ?: 465,
                smtpUser = p[Keys.SMTP_USER] ?: "",
                smtpPass = p[Keys.SMTP_PASS] ?: "",
                useStartTls = p[Keys.USE_STARTTLS] ?: false,
                useSsl = p[Keys.USE_SSL] ?: true,
                fromAddress = p[Keys.FROM_ADDRESS]?.ifEmpty { p[Keys.SMTP_USER] ?: "" } ?: "",
                toAddress = p[Keys.TO_ADDRESS] ?: "",
                senderFilter = p[Keys.SENDER_FILTER] ?: "",
                keywordFilter = p[Keys.KEYWORD_FILTER] ?: "",
                subjectPrefix = p[Keys.SUBJECT_PREFIX] ?: "[短信转发]",
                maxRetry = p[Keys.MAX_RETRY] ?: 5,
                signature = p[Keys.SIGNATURE] ?: ""
            )
            val next = block(current)
            p[Keys.ENABLED] = next.enabled
            p[Keys.SMTP_HOST] = next.smtpHost
            p[Keys.SMTP_PORT] = next.smtpPort
            p[Keys.SMTP_USER] = next.smtpUser
            p[Keys.SMTP_PASS] = next.smtpPass
            p[Keys.USE_STARTTLS] = next.useStartTls
            p[Keys.USE_SSL] = next.useSsl
            p[Keys.FROM_ADDRESS] = next.fromAddress
            p[Keys.TO_ADDRESS] = next.toAddress
            p[Keys.SENDER_FILTER] = next.senderFilter
            p[Keys.KEYWORD_FILTER] = next.keywordFilter
            p[Keys.SUBJECT_PREFIX] = next.subjectPrefix
            p[Keys.MAX_RETRY] = next.maxRetry
            p[Keys.SIGNATURE] = next.signature
        }
    }

    data class Snapshot(
        val enabled: Boolean = false,
        val smtpHost: String = "smtp.qq.com",
        val smtpPort: Int = 465,
        val smtpUser: String = "",
        val smtpPass: String = "",
        val useStartTls: Boolean = false,
        val useSsl: Boolean = true,
        val fromAddress: String = "",
        val toAddress: String = "",
        val senderFilter: String = "",
        val keywordFilter: String = "",
        val subjectPrefix: String = "[短信转发]",
        val maxRetry: Int = 5,
        val signature: String = ""
    ) {
        /** 是否符合过滤规则（号码、关键字） */
        fun matches(sender: String, body: String): Boolean {
            val senders = senderFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (senders.isNotEmpty() && senders.none { sender.contains(it, ignoreCase = true) }) {
                return false
            }
            val keywords = keywordFilter.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (keywords.isNotEmpty() && keywords.none { body.contains(it, ignoreCase = true) }) {
                return false
            }
            return true
        }

        val recipients: List<String>
            get() = toAddress.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
