package com.sim2all.smsforward.mail

import com.sim2all.smsforward.data.Settings
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Date
import java.util.Properties

/**
 * SMTP 直发封装（基于 JavaMail android-mail）。
 *
 * 用法：
 *   val sender = MailSender(snapshot)
 *   sender.send(sender = "10086", body = "您的验证码是 123456", receivedAt = Date())
 *
 * 一次发送只创建一个 Session，避免每次发送都重建。
 */
class MailSender(private val cfg: Settings.Snapshot) {

    private val session: Session by lazy {
        val props = Properties().apply {
            setProperty("mail.transport.protocol", "smtp")
            setProperty("mail.smtp.host", cfg.smtpHost)
            setProperty("mail.smtp.port", cfg.smtpPort.toString())
            setProperty("mail.smtp.auth", "true")
            setProperty("mail.smtp.connectiontimeout", "15000")
            setProperty("mail.smtp.timeout", "30000")
            setProperty("mail.smtp.writetimeout", "15000")

            if (cfg.useSsl) {
                setProperty("mail.smtp.ssl.enable", "true")
                setProperty("mail.smtp.ssl.checkserveridentity", "false")
                setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                setProperty("mail.smtp.socketFactory.port", cfg.smtpPort.toString())
                setProperty("mail.smtp.socketFactory.fallback", "false")
            }
            if (cfg.useStartTls) {
                setProperty("mail.smtp.starttls.enable", "true")
                setProperty("mail.smtp.starttls.required", "true")
            }
        }

        Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication =
                PasswordAuthentication(cfg.smtpUser, cfg.smtpPass)
        }).apply { debug = false }
    }

    /**
     * 实际发送一封邮件。
     *
     * @param sender 短信发送方号码
     * @param body   短信正文
     * @param receivedAt 接收时间
     * @param recipients 收件邮箱列表（缺省从 cfg.recipients 取）
     * @throws Exception 任何 SMTP/网络异常
     */
    fun send(
        sender: String,
        body: String,
        receivedAt: Date,
        recipients: List<String> = cfg.recipients
    ) {
        require(recipients.isNotEmpty()) { "收件邮箱为空" }

        val subject = buildString {
            append(cfg.subjectPrefix)
            append(" 来自 ")
            append(sender)
        }

        val text = buildString {
            append("发送方：").append(sender).append("\n")
            append("接收时间：").append(receivedAt).append("\n")
            append("内容：\n").append(body)
            if (cfg.signature.isNotEmpty()) {
                append("\n\n").append(cfg.signature)
            }
        }

        val msg = MimeMessage(session).apply {
            setFrom(InternetAddress(cfg.fromAddress.ifEmpty { cfg.smtpUser }))
            setRecipients(Message.RecipientType.TO, recipients.joinToString(","))
            setSubject(subject, "UTF-8")
            setText(text, "UTF-8")
            setSentDate(Date())
        }
        Transport.send(msg)
    }
}
