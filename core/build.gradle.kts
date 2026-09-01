import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// :core 是纯 Kotlin/JVM 库（无 Android UI/资源），产出单个 jar、单一变体，
// 手机端(:app)与手表端(:wear)都以 implementation(project(":core")) 直接依赖，
// 彻底绕开 android-library 的 debugRuntimeElements 变体歧义。

dependencies {
    // 领域模型与 FHIR 导出使用 kotlinx-serialization；算法(RiskEngine/Stats)纯 Kotlin + java.time(>=26 原生)
    // 1.7.3 与 Kotlin 2.1.x 兼容（1.11.0 是用 Kotlin 2.3.0 编译的，无法被 2.1.x 读取）
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
}

// 用构建环境已有的 JDK 21 编译，但统一发出 JVM 17 字节码：
// compileJava 与 compileKotlin 的 target 必须一致，否则 Gradle 报 JVM-target 不兼容。
// 仅设 target 不切 toolchain，避免自动下载 JDK 17。
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}
