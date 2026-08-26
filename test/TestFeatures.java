import com.silema.app.data.*;
import com.silema.app.engine.*;
import java.util.*;

public class TestFeatures {
    static long now = System.currentTimeMillis();
    static int fails = 0;

    public static void main(String[] args) {
        // ---- 1. 压力指数映射 ----
        int s10 = StressMath.INSTANCE.fromRmssd(10.0);
        int s100 = StressMath.INSTANCE.fromRmssd(100.0);
        int s50 = StressMath.INSTANCE.fromRmssd(50.0);
        check("stress_low_end", s10 >= 95, "rmssd10->" + s10);
        check("stress_high_end", s100 <= 5, "rmssd100->" + s100);
        check("stress_mid_monotonic", s10 > s50 && s50 > s100, "10:" + s10 + " 50:" + s50 + " 100:" + s100);
        int sClamp = StressMath.INSTANCE.fromRmssd(9999.0);
        check("stress_clamped", sClamp >= 0 && sClamp <= 100, "rmssd9999->" + sClamp);

        // ---- 2. Haversine：北京天安门→故宫约 1.6km 量级；同点=0 ----
        double same = Stats.INSTANCE.haversineMeters(39.9087, 116.3975, 39.9087, 116.3975);
        check("haversine_zero", Math.abs(same) < 1e-6, "" + same);
        // 纬度差 0.01° ≈ 1111.9m（赤道外近似）
        double d = Stats.INSTANCE.haversineMeters(39.90, 116.3975, 39.91, 116.3975);
        check("haversine_0.01lat", Math.abs(d - 1111.9) < 5.0, "d=" + d);

        // ---- 3. 周报：构造两周数据验证对比 ----
        List<VitalRecord> recs = new ArrayList<>();
        // 上周：心率均值 80（每天2次）
        for (int day = 8; day <= 14; day++) {
            long ts = now - day * 86400000L;
            recs.add(new VitalRecord("heart_rate", 78.0, ts, "manual"));
            recs.add(new VitalRecord("heart_rate", 82.0, ts + 3600000L, "manual"));
        }
        // 本周：心率均值 70（每天2次）
        for (int day = 1; day <= 7; day++) {
            long ts = now - day * 86400000L;
            recs.add(new VitalRecord("heart_rate", 68.0, ts, "manual"));
            recs.add(new VitalRecord("heart_rate", 72.0, ts + 3600000L, "manual"));
            // 本周睡眠：每天 8 小时
            recs.add(new VitalRecord("sleep", 8.0, ts, "manual"));
        }
        List<Workout> workouts = new ArrayList<>();
        workouts.add(new Workout("w1", "walk", now - 86400000L, 1800000L, 2000.0, 68.7, new ArrayList<>()));
        HealthReport.Weekly rep = HealthReport.INSTANCE.weekly(recs, workouts, now, 2);

        HealthReport.MetricWeek hr = rep.getMetrics().stream()
            .filter(m -> m.getType() == VitalType.HEART_RATE).findFirst().orElse(null);
        boolean hrOk = hr != null
            && Math.abs(hr.getThisWeekAvg() - 70.1538) < 0.01
            && Math.abs(hr.getLastWeekAvg() - 79.2) < 0.01
            && hr.getDeltaPct() != null && Math.abs(hr.getDeltaPct() + 11.4216) < 0.1;
        check("weekly_hr_compare", hrOk,
            hr == null ? "missing" : "this=" + hr.getThisWeekAvg() + " last=" + hr.getLastWeekAvg()
                + " delta=" + hr.getDeltaPct() + "%");

        boolean sleepOk = rep.getSleepAvgHours() != null && Math.abs(rep.getSleepAvgHours() - 8.0) < 0.01;
        check("weekly_sleep_avg", sleepOk, "sleep=" + rep.getSleepAvgHours());

        boolean workoutOk = rep.getWorkoutCount() == 1 && Math.abs(rep.getWorkoutKm() - 2.0) < 0.01;
        check("weekly_workouts", workoutOk, "count=" + rep.getWorkoutCount() + " km=" + rep.getWorkoutKm());

        boolean summaryOk = rep.getSummary().stream().anyMatch(l -> l.contains("静息心率"))
            && rep.getSummary().stream().anyMatch(l -> l.contains("睡眠"));
        check("weekly_summary_lines", summaryOk, "lines=" + rep.getSummary().size());

        System.out.println(fails == 0 ? "ALL_FEATURE_TESTS_PASSED" : ("FEATURE_TESTS_FAILED=" + fails));
        if (fails > 0) System.exit(1);
    }

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + " -> " + detail);
        if (!ok) fails++;
    }
}

