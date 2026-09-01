# ============================================================
# Silema App - ProGuard / R8 Rules
# ============================================================

# ---------- 数据模型（kotlinx-serialization） ----------
# 序列化类需要保留无参构造和字段名
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# kotlinx-serialization 核心
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# 项目数据模型
-keep @kotlinx.serialization.Serializable class com.silema.app.** { *; }
-keepclassmembers class com.silema.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Room 数据库 ----------
# Room 实体和 DAO
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ---------- Health Connect ----------
-dontwarn androidx.health.connect.client.**

# ---------- Compose ----------
# Compose 相关规则（Compose BOM 自带，这里兜底）
-dontwarn androidx.compose.**

# ---------- 蓝牙 / BLE ----------
-dontwarn android.bluetooth.**

# ---------- 通用：保留枚举 ----------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---------- 通用：保留 Parcelable ----------
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ---------- 通用：保留 JS 接口（如有 WebView） ----------
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---------- 调试信息（release 保留行号便于崩溃定位） ----------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
