# ============================================================
# Silema Wear OS - ProGuard / R8 Rules
# ============================================================

# ---------- 数据模型（kotlinx-serialization） ----------
-keepattributes *Annotation*, InnerClasses
-keep @kotlinx.serialization.Serializable class com.silema.app.** { *; }
-keepclassmembers class com.silema.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Wear Compose ----------
-dontwarn androidx.wear.compose.**

# ---------- 蓝牙 / BLE ----------
-dontwarn android.bluetooth.**

# ---------- 通用：保留枚举 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- 调试信息 ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
