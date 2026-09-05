plugins {
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.3.20" apply false
    id("com.google.dagger.hilt.android") version "2.56.2" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

// Dagger 自带的 kotlin-metadata-jvm 只读到 metadata 2.2；Kotlin 2.3.x 产出 2.3，
// 这里显式对齐版本，避免 kapt 解析 Kotlin 元数据时报错。
subprojects {
    configurations.configureEach {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.20")
        }
    }
}
