package com.silema.app.sos

import android.content.Intent
import android.net.Uri
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import com.silema.app.engine.RiskEngine

object Emergency {

    const val MEDICAL_NUMBER = "120"

    /** 拨号盘（无需权限，用户按一下拨出）。 */
    fun dialIntent(number: String): Intent =
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** 直接拨打（需要 CALL_PHONE 运行时授权）。 */
    fun callIntent(number: String): Intent =
        Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** 打开短信应用并预填收件人与正文（无需 SEND_SMS 权限，由用户确认发送）。 */
    fun smsIntent(phone: String, body: String): Intent =
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
            .putExtra("sms_body", body)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /**
     * 生成求救短信正文：包含风险等级、每条异常的实测值与时间。
     * 不使用 GPS 定位 —— 短信里写清楚「在哪里」由老人/家人口头补充，
     * 避免引入定位权限与室内定位失败的风险。
     */
    fun statusSummary(records: List<VitalRecord>): String {
        val assessment = com.silema.app.engine.RiskEngine.evaluate(records)
        val sb = StringBuilder()
        sb.appendLine("【紧急求助】请马上联系我！")
        sb.appendLine("健康状态评估：${assessment.level.label}")
        if (assessment.alerts.isEmpty()) {
            sb.appendLine("暂无自动测量数据，请尽快回电确认我的情况。")
        } else {
            for (alert in assessment.alerts.take(4)) {
                sb.appendLine("${alert.metric}：${alert.measured}（${alert.problem}）")
            }
        }
        val latest = records.maxByOrNull { it.timestampMillis }
        if (latest != null) {
            sb.appendLine("最后一条记录：${RiskEngine.clockText(latest.timestampMillis)}")
        }
        sb.append("我的位置：请回电向我确认。")
        return sb.toString()
    }

    fun primaryContactPhone(contacts: List<com.silema.app.data.Contact>): String? =
        contacts.firstOrNull()?.phone?.takeIf { it.isNotBlank() }

    fun levelLabel(level: RiskLevel): String = when (level) {
        RiskLevel.NORMAL -> "正常"
        RiskLevel.WATCH -> "注意"
        RiskLevel.WARNING -> "警告"
        RiskLevel.CRITICAL -> "危险"
    }

    fun typeOf(type: VitalType): String = type.displayName.substringBefore("(")
}
