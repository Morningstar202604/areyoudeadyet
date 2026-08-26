import com.silema.app.data.*;
import com.silema.app.engine.*;
import com.silema.app.ppg.PpgAnalyzer;
import com.silema.app.ble.BleCodec;
import java.util.*;

public class TestStats {
    static long now = System.currentTimeMillis();
    static RiskEngine eng = RiskEngine.INSTANCE;
    static int fails = 0;

    public static void main(String[] args) {
        // ---- 1. 数学原语 ----
        double mean = Stats.INSTANCE.mean(Arrays.asList(1.0, 2.0, 3.0, 4.0));
        check("mean", Math.abs(mean - 2.5) < 1e-9, "mean=" + mean);

        double std = Stats.INSTANCE.std(Arrays.asList(2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0));
        check("std_population", Math.abs(std - 2.0) < 1e-9, "std=" + std);

        double slope = Stats.INSTANCE.leastSquaresSlope(
            Arrays.asList(0.0, 1.0, 2.0, 3.0), Arrays.asList(1.0, 3.0, 5.0, 7.0));
        check("linreg_slope_exact", Math.abs(slope - 2.0) < 1e-9, "slope=" + slope);

        // ---- 2. 衍生血流动力学公式 ----
        double map = VitalsMath.INSTANCE.meanArterialPressure(120, 80);
        check("map_120_80", Math.abs(map - 93.3333) < 0.001, "MAP=" + map);

        check("shock_index", Math.abs(VitalsMath.INSTANCE.shockIndex(75, 150) - 0.5) < 1e-9,
              "SI=" + VitalsMath.INSTANCE.shockIndex(75, 150));

        // 引擎层：SI>=1.0 触发危险级休克指数告警
        Assessment rSi = eng.evaluate(mk(110, 100, 70, 97, 36.5), now);
        boolean siAlert = rSi.getAlerts().stream()
            .anyMatch(x -> x.getMetric().equals("休克指数") && x.getLevel() == RiskLevel.CRITICAL);
        check("si_critical_alert", siAlert, "level=" + rSi.getLevel());

        // MAP<65 且收缩压不低时触发警告（如 95/60 → MAP 71.7 不触发；用 92/55 → MAP 67.3 不触发；用 90/52 → MAP 64.7 触发）
        Assessment rMap = eng.evaluate(mk(60, 90, 52, 98, 36.5), now);
        boolean mapAlert = rMap.getAlerts().stream().anyMatch(x -> x.getMetric().equals("平均动脉压"));
        check("map_low_warning", mapAlert, "found=" + mapAlert);

        // ---- 3. 个人基线 z-score ----
        List<VitalRecord> baselineCase = new ArrayList<>();
        double[] priors = {116, 120, 122, 126, 130};
        for (int i = 0; i < priors.length; i++) {
            baselineCase.add(new VitalRecord("systolic", priors[i], now - (i + 1) * 86400000L, "manual"));
            baselineCase.add(new VitalRecord("diastolic", 78.0, now - (i + 1) * 86400000L, "manual"));
        }
        baselineCase.add(new VitalRecord("systolic", 140.0, now - 60000, "manual"));
        baselineCase.add(new VitalRecord("diastolic", 88.0, now - 60000, "manual"));
        Assessment rBase = eng.evaluate(baselineCase, now);
        boolean baseAlert = rBase.getAlerts().stream()
            .anyMatch(x -> x.getMetric().startsWith("基线偏差") && x.getProblem().contains("|z|"));
        check("baseline_zscore_watch", baseAlert, "alerts=" + rBase.getAlerts().size());

        // ---- 4. 趋势回归：连续一周收缩压每日+3mmHg ----
        List<VitalRecord> trendCase = new ArrayList<>();
        for (int d = 6; d >= 0; d--) {
            double v = 112 + (6 - d) * 3;
            trendCase.add(new VitalRecord("systolic", v, now - d * 86400000L - 3600000L * 8, "manual"));
            trendCase.add(new VitalRecord("systolic", v, now - d * 86400000L - 3600000L * 20, "manual"));
            trendCase.add(new VitalRecord("diastolic", 76.0, now - d * 86400000L - 3600000L * 8, "manual"));
            trendCase.add(new VitalRecord("heart_rate", 70.0, now - d * 86400000L - 3600000L * 8, "manual"));
            trendCase.add(new VitalRecord("spo2", 98.0, now - d * 86400000L - 3600000L * 8, "manual"));
            trendCase.add(new VitalRecord("temperature", 36.4, now - d * 86400000L - 3600000L * 8, "manual"));
        }
        Assessment rTrend = eng.evaluate(trendCase, now);
        boolean trendAlert = rTrend.getAlerts().stream()
            .anyMatch(x -> x.getMetric().startsWith("趋势·血压") && x.getLevel() == RiskLevel.WARNING);
        check("trend_regression_warning", trendAlert, "level=" + rTrend.getLevel());

        // ---- 5. PPG 分析器：合成 60bpm 脉搏波 ----
        PpgAnalyzer ppg = new PpgAnalyzer();
        int fps = 30;
        for (int f = 0; f < fps * 35; f++) {
            long tMs = f * 1000L / fps;
            double phase = (tMs % 1000) / 1000.0;
            double pulse = 900 * Math.exp(-Math.pow((phase - 0.15) / 0.05, 2))
                         + 300 * Math.exp(-Math.pow((phase - 0.40) / 0.08, 2)); // 主波+重搏波
            double noise = ((f % 7) - 3) * 2.5; // 确定性小噪声
            ppg.addSample(tMs, 5000 + pulse + noise);
        }
        PpgAnalyzer.Result pr = ppg.analyze();
        boolean ppgOk = pr != null && Math.abs(pr.getBpm() - 60.0) <= 4.0 && pr.getBeatCount() >= 20;
        check("ppg_synthetic_60bpm", ppgOk,
              pr == null ? "null" : ("bpm=" + pr.getBpm() + " beats=" + pr.getBeatCount()
                  + " rmssd=" + pr.getRmssdMs() + " conf=" + pr.getConfidence()));

        // 数据不足必须返回 null（诚实拒绝，不编数字）
        PpgAnalyzer shortPpg = new PpgAnalyzer();
        for (int f = 0; f < fps * 5; f++) shortPpg.addSample(f * 33L, 5000);
        check("ppg_reject_short_signal", shortPpg.analyze() == null, "short->null");

        // ---- 6. BLE 协议解析 ----
        byte[] hr8 = {0x00, 72};
        byte[] hr16 = {0x01, (byte) 0xB4, 0x00}; // uint16 LE = 180
        check("ble_hr_u8", BleCodec.INSTANCE.parseHeartRate(hr8) == 72.0, "" + BleCodec.INSTANCE.parseHeartRate(hr8));
        check("ble_hr_u16", BleCodec.INSTANCE.parseHeartRate(hr16) == 180.0, "" + BleCodec.INSTANCE.parseHeartRate(hr16));

        byte[] sfTemp = {(byte) 0x6D, (byte) 0xF1}; // SFLOAT 365 x 10^-1 = 36.5
        check("ble_sfloat_36_5", Double.compare(BleCodec.INSTANCE.sfloat(sfTemp, 0), 36.5) == 0,
              "" + BleCodec.INSTANCE.sfloat(sfTemp, 0));

        byte[] sfNeg = {(byte) 0xEF, (byte) 0xFE}; // SFLOAT -273 x 10^-1 = -27.3
        check("ble_sfloat_negative", Double.compare(BleCodec.INSTANCE.sfloat(sfNeg, 0), -27.3) == 0,
              "" + BleCodec.INSTANCE.sfloat(sfNeg, 0));

        byte[] bp = {0x00, 0x78, 0x00, 0x50, 0x00, 0x00, 0x00}; // SYS=120 DIA=80 MAP=0
        List<Double> parsedBp = BleCodec.INSTANCE.parseBloodPressure(bp);
        boolean bpOk = parsedBp != null && parsedBp.get(0) == 120.0 && parsedBp.get(1) == 80.0;
        check("ble_bp_parse", bpOk, "" + parsedBp);

        System.out.println(fails == 0 ? "ALL_STATS_TESTS_PASSED" : ("STATS_TESTS_FAILED=" + fails));
        if (fails > 0) System.exit(1);
    }

    static List<VitalRecord> mk(double hr, double sys, double dia, double spo2, double temp) {
        List<VitalRecord> l = new ArrayList<>();
        l.add(new VitalRecord("heart_rate", hr, now - 1000, "manual"));
        l.add(new VitalRecord("systolic", sys, now - 1000, "manual"));
        l.add(new VitalRecord("diastolic", dia, now - 1000, "manual"));
        l.add(new VitalRecord("spo2", spo2, now - 1000, "manual"));
        l.add(new VitalRecord("temperature", temp, now - 2000, "manual"));
        return l;
    }

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + " -> " + detail);
        if (!ok) fails++;
    }
}








