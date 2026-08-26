import com.silema.app.data.*;
import com.silema.app.engine.*;
import java.util.*;

public class TestEngine {
    static long now = System.currentTimeMillis();
    static RiskEngine eng = RiskEngine.INSTANCE;
    static int fails = 0;

    public static void main(String[] args) {
        // CASE 1: 全部正常
        Assessment r1 = eng.evaluate(mk(72, 120, 78, 98, 36.5), now);
        check("normal_all", r1.getLevel() == RiskLevel.NORMAL,
              "level=" + r1.getLevel() + " alerts=" + r1.getAlerts().size());

        // CASE 2: 高血压危象
        Assessment r2 = eng.evaluate(mk(80, 185, 115, 97, 36.5), now);
        check("bp_critical", r2.getLevel() == RiskLevel.CRITICAL && !r2.getAlerts().isEmpty(),
              "level=" + r2.getLevel());
        System.out.println("      first=" + r2.getAlerts().get(0).getProblem());

        // CASE 3: 低血氧危险
        Assessment r3 = eng.evaluate(mk(85, 125, 80, 88, 36.5), now);
        check("spo2_critical", r3.getLevel() == RiskLevel.CRITICAL, "level=" + r3.getLevel());

        // CASE 4: 组合规则 低血压+心跳快 => 危险
        Assessment r4 = eng.evaluate(mk(108, 95, 62, 96, 36.5), now);
        boolean hasCombo4 = r4.getAlerts().stream().anyMatch(x -> x.getMetric().contains("+"));
        check("combo_shock", r4.getLevel() == RiskLevel.CRITICAL && hasCombo4,
              "level=" + r4.getLevel() + " comboFound=" + hasCombo4);

        // CASE 5: 空数据 => 警告（无数据不可沉默）
        Assessment r5 = eng.evaluate(new ArrayList<>(), now);
        check("empty_warns", r5.getLevel() == RiskLevel.WARNING,
              "level=" + r5.getLevel());

        // CASE 6: 连续3次心率超标 => 升级为警告
        List<VitalRecord> streak = new ArrayList<>();
        for (int i = 3; i >= 1; i--) {
            streak.add(new VitalRecord("heart_rate", 105d, now - i * 3600000L, "manual"));
        }
        List<VitalRecord> base6 = mk(70, 118, 76, 97, 36.4);
        for (VitalRecord r : base6) {
            if (!r.getTypeId().equals("heart_rate")) streak.add(r);
        }
        Assessment r6 = eng.evaluate(streak, now);
        boolean escalated = r6.getAlerts().stream()
            .anyMatch(x -> x.getProblem().contains("连续 3 次"));
        check("streak_escalation", escalated, "escalated=" + escalated);

        // CASE 7: 每条告警必须三段齐全（是什么/为什么/做什么）
        boolean ok7 = threeParts(r6) && threeParts(eng.evaluate(mk(160, 190, 120, 85, 40.1), now));
        check("alerts_three_parts", ok7, "");

        System.out.println(fails == 0 ? "ALL_TESTS_PASSED" : ("TESTS_FAILED=" + fails));
        if (fails > 0) System.exit(1);
    }

    static boolean threeParts(Assessment a) {
        return a.getAlerts().stream().allMatch(x ->
            !x.getProblem().isEmpty() && !x.getWhy().isEmpty() && !x.getAction().isEmpty());
    }

    static List<VitalRecord> mk(double hr, double sys, double dia, double spo2, double temp) {
        List<VitalRecord> l = new ArrayList<>();
        l.add(new VitalRecord("heart_rate", hr, now - 1000, "manual"));
        l.add(new VitalRecord("systolic", sys, now - 1000, "manual"));
        l.add(new VitalRecord("diastolic", dia, now - 1000, "manual"));
        l.add(new VitalRecord("spo2", spo2, now - 1000, "manual"));
        l.add(new VitalRecord("temperature", temp, now - 1000, "manual"));
        return l;
    }

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + " -> " + detail);
        if (!ok) fails++;
    }
}

