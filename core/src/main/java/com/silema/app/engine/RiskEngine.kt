package com.silema.app.engine

import com.silema.app.data.AlertItem
import com.silema.app.data.Assessment
import com.silema.app.data.RiskLevel
import com.silema.app.data.VitalRecord
import com.silema.app.data.VitalType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * 规则型风险评估引擎。
 *
 * 设计原则（与"空泛健康建议"的区别）：
 * 1. 每条输出必须回答三个问题：是什么问题 / 为什么危险 / 现在具体做什么。
 * 2. 阈值来自公开的医学共识范围，宁严勿松；不确定时提高关注级别而不是沉默。
 * 3. 支持组合规则（多指标联合判断）与连续超标升级（streak escalation）。
 */
object RiskEngine {

    private const val BASELINE_WINDOW_MS = 14L * 24 * 3600_000   // 个人基线回看两周
    private const val TREND_WINDOW_MS = 21L * 24 * 3600_000      // 趋势回归窗口三周

    fun evaluate(allRecords: List<VitalRecord>, nowMillis: Long = System.currentTimeMillis()): Assessment {
        val zone = ZoneId.systemDefault()
        val todayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()

        val byType: Map<VitalType, List<VitalRecord>> = allRecords
            .mapNotNull { r -> r.type?.let { it to r } }
            .groupBy({ it.first }, { it.second })

        val alerts = mutableListOf<AlertItem>()

        evaluateBloodPressure(byType, alerts)
        evaluateHeartRate(byType, alerts)
        evaluateSpO2(byType, alerts)
        evaluateTemperature(byType, alerts)
        applyComboRules(byType, alerts)
        applyStreakEscalation(byType, alerts)
        applyDerivedHemodynamics(byType, alerts)
        applyPersonalStatistics(byType, alerts, nowMillis)
        applyTrendAnalysis(byType, alerts, nowMillis)

        val missingToday = coreMetricsMissingToday(byType, todayStart)
        val hasNoDataAtAll = allRecords.isEmpty()

        if (hasNoDataAtAll) {
            alerts += AlertItem(
                level = RiskLevel.WARNING,
                metric = "数据",
                measured = "无任何测量记录",
                problem = "还没有录入任何生命体征数据",
                why = "没有数据就无法发现危险信号。老人突发状况往往在无人测量的间隙发生。",
                action = "现在就到「录入」页测量并填写一次血压、心率和血氧"
            )
        } else if (missingToday.isNotEmpty()) {
            alerts += AlertItem(
                level = RiskLevel.WATCH,
                metric = "数据",
                measured = "今日缺项：" + missingToday.joinToString("、"),
                problem = "今天还没有测量全部核心指标",
                why = "漏测会让正在恶化的指标逃过预警。",
                action = "补测缺失项：${missingToday.joinToString("、")}"
            )
        }

        val overall = alerts.fold(RiskLevel.NORMAL) { acc, item -> acc.maxWith(item.level) }
        return Assessment(overall, alerts.sortedByDescending { it.level.rank }, nowMillis, missingToday)
    }

    private fun latestOf(byType: Map<VitalType, List<VitalRecord>>, type: VitalType): VitalRecord? =
        byType[type]?.maxByOrNull { it.timestampMillis }

    private fun fmt(type: VitalType, value: Double): String =
        when (type) {
            VitalType.TEMPERATURE -> String.format("%.1f", value)
            else -> if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.1f", value)
        } + type.unit

    // ---- 血压：收缩压/舒张压合并评估，输出单条最高级别告警 ----
    private fun evaluateBloodPressure(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        val sys = latestOf(byType, VitalType.SYSTOLIC)?.value ?: return
        val dia = latestOf(byType, VitalType.DIASTOLIC)?.value ?: return

        val finding: AlertItem? = when {
            sys >= 180 || dia >= 110 -> AlertItem(
                RiskLevel.CRITICAL, "血压", "收缩压 ${sys.toInt()}/舒张压 ${dia.toInt()} mmHg",
                "血压达到高血压危象水平（≥180/≥110）",
                "这个水平的血压可能随时引发脑出血、主动脉夹层或急性心衰，几分钟内就会恶化。",
                "立即拨打 120 或马上送医，坐下休息不要走动，不要自行加倍服药等医生指示"
            )
            sys >= 160 || dia >= 100 -> AlertItem(
                RiskLevel.WARNING, "血压", "收缩压 ${sys.toInt()}/舒张压 ${dia.toInt()} mmHg",
                "血压达到 2 级高血压水平（160-179 或 100-109）",
                "持续在这个水平会显著升高脑卒中风险，不能只靠感觉判断有没有事。",
                "安静休息 15 分钟后复测一次；若仍高于此水平，今天之内联系社区医生或就诊调整用药"
            )
            sys >= 140 || dia >= 90 -> AlertItem(
                RiskLevel.WATCH, "血压", "收缩压 ${sys.toInt()}/舒张压 ${dia.toInt()} mmHg",
                "血压达到 1 级高血压水平（140-159 或 90-99）",
                "多数人对这个水平毫无感觉，但它每天都在损伤血管和肾脏。",
                "早晚各复测一次记录下来；连续 3 天超标必须就医，不要等症状"
            )
            sys < 90 || dia < 55 -> AlertItem(
                RiskLevel.CRITICAL, "血压", "收缩压 ${sys.toInt()}/舒张压 ${dia.toInt()} mmHg",
                "血压过低（收缩压 <90 或舒张压 <55）",
                "低血压会导致大脑供血不足，老人容易跌倒、晕厥，严重时是休克前兆。",
                "立即平卧抬高双腿，观察是否头晕冷汗；若伴随意识模糊或心率异常加快，立即拨打 120"
            )
            sys in 90.0..99.9 || dia in 55.0..59.9 -> AlertItem(
                RiskLevel.WATCH, "血压", "收缩压 ${sys.toInt()}/舒张压 ${dia.toInt()} mmHg",
                "血压偏低",
                "偏低血压在服用降压药的老人身上可能意味着药物过量。",
                "今天减少起身动作防止跌倒，复测两次，若持续低于此区间联系医生调药"
            )
            else -> null
        }
        if (finding != null) out += finding
    }

    // ---- 心率（静息）----
    private fun evaluateHeartRate(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        val hr = latestOf(byType, VitalType.HEART_RATE)?.value ?: return
        val finding: AlertItem? = when {
            hr >= 150 -> AlertItem(
                RiskLevel.CRITICAL, "心率", "静息心率 ${hr.toInt()} 次/分",
                "静息心率严重过快（≥150）",
                "可能是房颤、室上速等心律失常，也可能提示内出血、感染性休克，心脏在无效空转。",
                "立即停止活动坐下，拨打 120；如果本人感到胸闷眼前发黑，让身边人陪同等待救护车"
            )
            hr in 121.0..149.9 -> AlertItem(
                RiskLevel.WARNING, "心率", "静息心率 ${hr.toInt()} 次/分",
                "静息心率明显过快（121-149）",
                "静息状态下心跳这么快，常见原因是发热、脱水、心律失常或心衰加重。",
                "先测体温和血压排除发热；15 分钟后复测，仍 >120 就当天就医"
            )
            hr in 100.0..120.9 -> AlertItem(
                RiskLevel.WATCH, "心率", "静息心率 ${hr.toInt()} 次/分",
                "静息心率偏快（100-120）",
                "偶尔一次可能与情绪、咖啡因有关，但反复出现提示心脏或代谢问题。",
                "平静坐 10 分钟后复测；今天之内再测 2 次并记录"
            )
            hr <= 45 -> AlertItem(
                RiskLevel.CRITICAL, "心率", "静息心率 ${hr.toInt()} 次/分",
                "静息心率严重过缓（≤45）",
                "可能是病窦综合征或传导阻滞，心跳慢到这个程度大脑供血不足会突然晕厥。",
                "观察有无头晕黑矇乏力；无论有无感觉，24 小时内做心电图检查，出现晕厥立即拨打 120"
            )
            hr in 45.0..49.9 -> AlertItem(
                RiskLevel.WARNING, "心率", "静息心率 ${hr.toInt()} 次/分",
                "静息心率过缓（46-49）",
                "长期服药（如倍他乐克类）的老人出现心动过缓可能是药物过量。",
                "回忆是否漏服或重复服药，复测 2 次；持续低于 50 联系医生评估是否调药"
            )
            else -> null
        }
        if (finding != null) out += finding
    }

    // ---- 血氧饱和度 ----
    private fun evaluateSpO2(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        val spo2 = latestOf(byType, VitalType.SPO2)?.value ?: return
        val finding: AlertItem? = when {
            spo2 < 90 -> AlertItem(
                RiskLevel.CRITICAL, "血氧", "血氧饱和度 ${spo2.toInt()}%",
                "血氧低于 90%（呼吸衰竭水平）",
                "身体器官正在缺氧。肺炎、心衰、慢阻肺急性发作都会到这个水平，拖久了会造成不可逆损伤。",
                "坐起保持呼吸道通畅，有制氧机立即吸氧并拨打 120；5 分钟后复测，仍低于 90 不要犹豫"
            )
            spo2 <= 93 -> AlertItem(
                RiskLevel.WARNING, "血氧", "血氧饱和度 ${spo2.toInt()}%",
                "血氧偏低（90-93%）",
                "这是肺部功能下降的明确信号，老年人肺炎常常不发烧，唯一线索就是血氧下降。",
                "手指保暖后复测 2 次（手凉会导致假性偏低）；确认仍 ≤93 当天就医拍胸片"
            )
            spo2 <= 95 -> AlertItem(
                RiskLevel.WATCH, "血氧", "血氧饱和度 ${spo2.toInt()}%",
                "血氧处于临界值（94-95%）",
                "正常老人静息血氧应 ≥96%，降到临界值需要警惕早期肺部感染。",
                "1 小时后复测；同时注意有没有咳嗽气短，有任一症状直接就医"
            )
            else -> null
        }
        if (finding != null) out += finding
    }

    // ---- 体温 ----
    private fun evaluateTemperature(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        val temp = latestOf(byType, VitalType.TEMPERATURE)?.value ?: return
        val finding: AlertItem? = when {
            temp >= 39.5 -> AlertItem(
                RiskLevel.CRITICAL, "体温", "体温 $temp ℃",
                "高热（≥39.5℃）",
                "老人高热极易引发脱水、谵妄和高热惊厥，且往往是重症感染的信号。",
                "物理降温+少量多次补水，立即拨打 120 或送医；超过 40℃ 不等交通直接叫车去医院"
            )
            temp >= 38.5 -> AlertItem(
                RiskLevel.WARNING, "体温", "体温 $temp ℃",
                "中高度发热（38.5-39.4℃）",
                "老人免疫反应弱，能烧到这个温度说明感染不轻。",
                "4 小时内复测体温；按医嘱剂量使用退烧药，出现意识模糊、尿量减少立即就医"
            )
            temp >= 37.3 -> AlertItem(
                RiskLevel.WATCH, "体温", "体温 $temp ℃",
                "低热（37.3-38.4℃）",
                "老人低热可能是肺炎、尿路感染的早期表现，容易被当成\"有点累\"忽略掉。",
                "每 4 小时测一次体温并记录；24 小时不退热或精神变差就就医，不要硬扛"
            )
            temp <= 35.0 && temp > 30.0 -> AlertItem(
                RiskLevel.CRITICAL, "体温", "体温 $temp ℃",
                "体温过低（≤35℃）",
                "低体温症在老人身上常由甲减、败血症或保暖不足引起，同样致命。",
                "立即加衣盖被转移到温暖环境，喝温水；复测仍 ≤35 拨打 120"
            )
            temp in 35.01..35.99 -> AlertItem(
                RiskLevel.WATCH, "体温", "体温 $temp ℃",
                "体温偏低",
                "体温偏低会影响免疫和心脏功能，老人基础代谢下降更明显。",
                "确认测温方式正确后复测；注意保暖，1 小时后再测一次"
            )
            else -> null
        }
        if (finding != null) out += finding
    }

    // ---- 组合规则：单一指标正常 ≠ 安全 ----
    private fun applyComboRules(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        val sys = latestOf(byType, VitalType.SYSTOLIC)?.value
        val hr = latestOf(byType, VitalType.HEART_RATE)?.value
        val spo2 = latestOf(byType, VitalType.SPO2)?.value
        val temp = latestOf(byType, VitalType.TEMPERATURE)?.value

        if (sys != null && hr != null && sys <= 100 && hr >= 100) {
            out += AlertItem(
                RiskLevel.CRITICAL, "血压+心率", "收缩压 ${sys.toInt()} mmHg，心率 ${hr.toInt()} 次/分",
                "低血压合并心动过快",
                "这是休克的典型代偿组合：血压掉下去，心脏拼命跳来维持供血。单独看两个数字都\"不算太糟\"，合在一起很危险。",
                "立即拨打 120，平卧不要站立走动；回想今天是否有呕血黑便大量腹泻或胸痛"
            )
        }
        if (spo2 != null && hr != null && spo2 <= 93 && hr >= 110) {
            out += AlertItem(
                RiskLevel.CRITICAL, "血氧+心率", "血氧 ${spo2.toInt()}%，心率 ${hr.toInt()} 次/分",
                "缺氧合并心跳加速",
                "身体缺氧迫使心脏加速泵血，说明呼吸系统已经扛不住了。",
                "立即拨打 120；有氧气设备马上吸氧，保持坐位"
            )
        }
        if (temp != null && hr != null && temp >= 38.0 && hr >= 110) {
            out += AlertItem(
                RiskLevel.WARNING, "体温+心率", "体温 $temp ℃，心率 ${hr.toInt()} 次/分",
                "发热合并心率明显增快",
                "发热时心率通常每升高 1℃ 增快约 10 次，超出这个比例提示脱水、脓毒症或心脏负担过重。",
                "补充水分电解质，2 小时内复测体温和心率；心率继续上升或出现寒战意识差立即就医"
            )
        }
    }

    /**
     * 衍生血流动力学指标：MAP（平均动脉压）、SI（休克指数）、PP（脉压差）。
     * 这些不是新数据，而是用公式从已有测量里"榨"出来的第二层信息。
     */
    private fun applyDerivedHemodynamics(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        val sys = latestOf(byType, VitalType.SYSTOLIC)?.value ?: return
        val dia = latestOf(byType, VitalType.DIASTOLIC)?.value ?: return
        val hr = latestOf(byType, VitalType.HEART_RATE)?.value

        val map = VitalsMath.meanArterialPressure(sys, dia)
        val pp = VitalsMath.pulsePressure(sys, dia)

        // MAP < 65：即使血压数字看起来"还行"，灌注也可能不够（尤其服用降压药者）
        if (map < 65 && sys >= 90) {
            out += AlertItem(
                RiskLevel.WARNING, "平均动脉压", "MAP ${String.format("%.0f", map)} mmHg",
                "平均动脉压低于 65（计算式：低压 + (高压-低压)/3）",
                "MAP 才是器官实际感受到的灌注压力。它低于 65 意味着肾脏、大脑可能长期处于供血不足状态。",
                "今天避免站立过久和热水澡；记录三次不同时间的血压，尽快带记录就诊评估用药"
            )
        }
        if (pp >= 65) {
            out += AlertItem(
                RiskLevel.WATCH, "脉压差", "脉压差 ${pp.toInt()} mmHg",
                "脉压差增宽（≥65）",
                "高压与低压差距过大，通常反映大动脉硬化，是老年人脑卒中风险的独立信号。",
                "按医嘱规律服药即可，不必恐慌；下次就诊主动告知医生这个数值"
            )
        }

        if (hr != null) {
            val si = VitalsMath.shockIndex(hr, sys)
            when {
                si >= 1.0 -> out += AlertItem(
                    RiskLevel.CRITICAL, "休克指数", "SI ${String.format("%.2f", si)}（心率${hr.toInt()}/收缩压${sys.toInt()}）",
                    "休克指数 ≥1.0",
                    "心率与收缩压的比值超过 1，说明心脏拼命代偿也维持不住压力，是显性休克的量化证据。",
                    "立即拨打 120，平卧抬高双腿，不要自行走动"
                )
                si >= 0.85 -> out += AlertItem(
                    RiskLevel.WARNING, "休克指数", "SI ${String.format("%.2f", si)}（心率${hr.toInt()}/收缩压${sys.toInt()}）",
                    "休克指数 0.85-0.99",
                    "该区间提示早期失代偿：可能脱水、内出血或感染性休克前期，症状往往还没出现。",
                    "立即补水休息并复测；若复测仍 >0.85 或伴冷汗乏力，当天就医"
                )
                si >= 0.72 -> out += AlertItem(
                    RiskLevel.WATCH, "休克指数", "SI ${String.format("%.2f", si)}（心率${hr.toInt()}/收缩压${sys.toInt()}）",
                    "休克指数偏高（0.72-0.84）",
                    "老年人处于此区间常见原因是脱水或降压药过量，值得盯住。",
                    "补充水分后 2 小时复测一次；今天少做体力活动"
                )
            }
        }
    }

    /**
     * 个人基线 z-score：和"标准人群范围"相比之外，
     * 更重要的是和自己近两周的均值比 —— |z|≥2.5 说明这次读数对本人而言极不寻常。
     */
    private fun applyPersonalStatistics(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>,
        nowMillis: Long
    ) {
        val windowStart = nowMillis - BASELINE_WINDOW_MS
        for (type in listOf(VitalType.HEART_RATE, VitalType.SYSTOLIC, VitalType.DIASTOLIC, VitalType.SPO2, VitalType.TEMPERATURE)) {
            val series = byType[type]?.sortedBy { it.timestampMillis } ?: continue
            if (series.size < 6) continue
            val latest = series.last()
            if (nowMillis - latest.timestampMillis > 36L * 3600_000) continue
            val prior = series.dropLast(1)
                .filter { it.timestampMillis in windowStart until latest.timestampMillis }
                .takeLast(10)
            if (prior.size < 5) continue

            val mu = Stats.mean(prior.map { it.value })
            val sigma = Stats.std(prior.map { it.value })
            if (sigma < MIN_SIGMA(type)) continue
            val z = Stats.zScore(latest.value, mu, sigma)
            if (abs(z) < 2.5) continue

            val base = baseMetricName(type)
            // 已有更高级别的同指标告警时不重复打扰
            if (out.any { it.metric == base && it.level.rank >= RiskLevel.WARNING.rank }) continue

            val direction = if (z > 0) "高于" else "低于"
            out += AlertItem(
                RiskLevel.WATCH, "基线偏差·$base",
                "${fmt(type, latest.value)}（你近两周均值 ${String.format("%.1f", mu)}，${direction}均值 ${String.format("%+.1f", z)} 个标准差）",
                "本次读数显著偏离你自己的基线（|z|≥2.5）",
                "对你个人而言，这次变化在统计上属于罕见事件（约千分之六概率自然出现），优先考虑身体真的发生了变化而不是仪器误差。",
                "今天再测两次确认是否复现；若连续偏离，带着这份记录咨询医生"
            )
        }
    }

    /** 不同指标的基线噪声下限，防止在恒定值上算出天文 z 分数。 */
    private fun MIN_SIGMA(type: VitalType): Double = when (type) {
        VitalType.TEMPERATURE -> 0.15
        VitalType.SPO2 -> 0.8
        else -> 3.0
    }

    /**
     * 趋势分析：对最近一段时间的日均值做最小二乘回归，
     * 斜率超阈值说明存在持续性单方向漂移（比单次超标更早发现问题）。
     */
    private fun applyTrendAnalysis(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>,
        nowMillis: Long
    ) {
        val windowStart = nowMillis - TREND_WINDOW_MS

        fun dailyAverages(type: VitalType): List<Pair<Double, Double>> {
            val zone = java.time.ZoneId.systemDefault()
            return byType[type].orEmpty()
                .filter { it.timestampMillis in windowStart..nowMillis }
                .groupBy { java.time.Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate().toEpochDay() }
                .mapNotNull { (day, recs) ->
                    if (recs.size < 2) null else day.toDouble() to Stats.mean(recs.map { it.value })
                }
                .sortedBy { it.first }
        }

        fun addTrendAlert(type: VitalType, slope: Double, level: RiskLevel, unitPerDay: String, problem: String, why: String, action: String) {
            val base = baseMetricName(type)
            if (out.any { it.metric.startsWith("趋势") }) return
            out += AlertItem(
                level, "趋势·$base",
                "斜率 ${String.format("%+.2f", slope)} $unitPerDay（近三周日均值回归）",
                problem,
                why,
                action
            )
        }

        val sysPts = dailyAverages(VitalType.SYSTOLIC)
        if (sysPts.size >= 5) {
            val slope = Stats.leastSquaresSlope(sysPts.map { it.first }, sysPts.map { it.second })
            if (slope >= 1.2) addTrendAlert(
                VitalType.SYSTOLIC, slope, RiskLevel.WARNING, "mmHg/天",
                "收缩压呈持续上升趋势",
                "按当前斜率一周会爬升约 ${String.format("%.0f", slope * 7)} mmHg。趋势性上升往往先于症状出现，是调整生活方式或用药的窗口期。",
                "每天固定时间测早晚各一次共 3 天验证趋势；无论结果如何，本周内带记录就诊"
            )
        }
        val hrPts = dailyAverages(VitalType.HEART_RATE)
        if (hrPts.size >= 5) {
            val slope = Stats.leastSquaresSlope(hrPts.map { it.first }, hrPts.map { it.second })
            if (slope >= 0.5) addTrendAlert(
                VitalType.HEART_RATE, slope, RiskLevel.WATCH, "次/分·天",
                "静息心率持续走高",
                "静息心率逐日上升可能与脱水、贫血、甲状腺问题或心功能变化有关。",
                "保证饮水与睡眠后观察 3 天；仍继续上升则就医查血常规和心电图"
            )
        }
        val spo2Pts = dailyAverages(VitalType.SPO2)
        if (spo2Pts.size >= 5) {
            val slope = Stats.leastSquaresSlope(spo2Pts.map { it.first }, spo2Pts.map { it.second })
            if (slope <= -0.12) addTrendAlert(
                VitalType.SPO2, slope, RiskLevel.WARNING, "%/天",
                "血氧呈缓慢下降趋势",
                "日均血氧持续下滑是肺部问题的早期曲线，老年肺炎常常就是这样被数据先发现的。",
                "立即加测一次确认设备读数稳定；24 小时内就医拍胸片，不要等发烧"
            )
        }
    }


    private fun applyStreakEscalation(
        byType: Map<VitalType, List<VitalRecord>>,
        out: MutableList<AlertItem>
    ) {
        for ((type, records) in byType) {
            if (type == VitalType.STEPS) continue
            val sorted = records.sortedByDescending { it.timestampMillis }.take(3)
            if (sorted.size < 3) continue
            if (sorted.all { isElevated(type, it.value) }) {
                val existing = out.filter { it.metric.contains(baseMetricName(type)) }
                val target = existing.maxByOrNull { it.level.rank }
                if (target != null && target.level == RiskLevel.WATCH) {
                    out.remove(target)
                    out += target.copy(
                        level = RiskLevel.WARNING,
                        problem = target.problem + "（连续 3 次测量均超标）",
                        why = target.why + " 连续三次都在同一侧偏离，基本可以排除偶然误差。",
                        action = target.action + " 已满足\"反复超标\"条件，不要再观望，按上述行动执行"
                    )
                }
            }
        }
    }

    private fun baseMetricName(type: VitalType): String = when (type) {
        VitalType.SYSTOLIC, VitalType.DIASTOLIC -> "血压"
        VitalType.HEART_RATE -> "心率"
        VitalType.SPO2 -> "血氧"
        VitalType.TEMPERATURE -> "体温"
        VitalType.STEPS -> "步数"
        VitalType.SLEEP -> "睡眠"
        VitalType.STRESS -> "压力"
    }

    /** 判断某次测量是否已越过"值得注意"的下限（用于 streak 判断）。 */
    private fun isElevated(type: VitalType, v: Double): Boolean = when (type) {
        VitalType.SYSTOLIC -> v >= 140 || v < 100
        VitalType.DIASTOLIC -> v >= 90 || v < 60
        VitalType.HEART_RATE -> v >= 100 || v <= 50
        VitalType.SPO2 -> v <= 95
        VitalType.TEMPERATURE -> v >= 37.3 || v <= 36.0
        VitalType.STEPS, VitalType.SLEEP, VitalType.STRESS -> false
    }

    /** 今天还缺哪些核心指标（血压两项算一项）。 */
    private fun coreMetricsMissingToday(
        byType: Map<VitalType, List<VitalRecord>>,
        todayStartMillis: Long
    ): List<String> {
        val missing = mutableListOf<String>()
        val bpDone = listOf(VitalType.SYSTOLIC, VitalType.DIASTOLIC).all { t ->
            byType[t].orEmpty().any { it.timestampMillis >= todayStartMillis }
        }
        if (!bpDone) missing.add("血压")
        for (t in listOf(VitalType.HEART_RATE, VitalType.SPO2)) {
            if (byType[t].orEmpty().none { it.timestampMillis >= todayStartMillis }) {
                missing.add(t.displayName.substringBefore("("))
            }
        }
        return missing
    }

    /** 单指标快速分级（与上方评估规则保持同一套阈值），用于卡片描边。 */
    fun metricLevel(type: VitalType, v: Double): RiskLevel = when (type) {
        VitalType.HEART_RATE -> when {
            v >= 150 || v <= 45 -> RiskLevel.CRITICAL
            v >= 121 || v < 50 -> RiskLevel.WARNING
            v >= 100 -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }
        VitalType.SYSTOLIC -> when {
            v >= 180 || v < 90 -> RiskLevel.CRITICAL
            v >= 160 -> RiskLevel.WARNING
            v >= 140 || v < 100 -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }
        VitalType.DIASTOLIC -> when {
            v >= 110 || v < 55 -> RiskLevel.CRITICAL
            v >= 100 -> RiskLevel.WARNING
            v >= 90 || v < 60 -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }
        VitalType.SPO2 -> when {
            v < 90 -> RiskLevel.CRITICAL
            v <= 93 -> RiskLevel.WARNING
            v <= 95 -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }
        VitalType.TEMPERATURE -> when {
            v >= 39.5 || (v <= 35.0 && v > 30.0) -> RiskLevel.CRITICAL
            v >= 38.5 -> RiskLevel.WARNING
            v >= 37.3 || v < 36.0 -> RiskLevel.WATCH
            else -> RiskLevel.NORMAL
        }
        VitalType.STEPS -> RiskLevel.NORMAL
        VitalType.SLEEP -> RiskLevel.NORMAL
        VitalType.STRESS -> RiskLevel.NORMAL
    }

    fun relativeTime(timestampMillis: Long, nowMillis: Long): String {
        val diffMin = (nowMillis - timestampMillis) / 60000L
        return when {
            diffMin < 1 -> "刚刚"
            diffMin < 60 -> "$diffMin 分钟前"
            diffMin < 60 * 24 -> "${diffMin / 60} 小时前"
            else -> "${diffMin / (60 * 24)} 天前"
        }
    }

    fun clockText(timestampMillis: Long): String {
        val zdt = Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault())
        return String.format("%02d-%02d %02d:%02d", zdt.monthValue, zdt.dayOfMonth, zdt.hour, zdt.minute)
    }
}
