import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.silema.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.silema.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "0.5.0"
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProps["storeFile"] as String)
                storePassword = keystoreProps["storePassword"] as String
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 算法测试位于项目根 test/，默认不在 Android 单元测试源集内，Gradle 不会执行。
    // 注册为 unit-test 源集，使其在发布门禁中可被实际运行。
    sourceSets {
        getByName("test") {
            // 算法测试位于项目根 test/，AGP 的 srcDir 相对 module 目录，需用 rootDir 指回项目根
            java.srcDir(rootDir.resolve("test"))
        }
    }

    // 用 Gradle 管理的 runtime classpath 实际跑 3 个算法测试程序（main 程序，非 JUnit）。
    // 任一程序校验失败会以非零退出，使门禁失败。
    // 根 test/ 的算法程序是带 main 的独立 Java 程序。显式编译 test 源集并以
    // 「test 编译产物 + 主代码 runtime classpath（含 kotlin-stdlib + android.jar）」作为运行时 classpath 实际运行。
    afterEvaluate {
        val testJavaClasses = "build/intermediates/javac/debugUnitTest/compileDebugUnitTestJavaWithJavac/classes"
        val testKotlinClasses = "build/tmp/kotlin-classes/debugUnitTest"
        val compileTest = tasks.named("compileDebugUnitTestJavaWithJavac")
        listOf("TestEngine", "TestStats", "TestFeatures").forEach { main ->
            tasks.register("runAlgo${main.removePrefix("Test")}", JavaExec::class) {
                dependsOn(compileTest)
                classpath = files(
                    testJavaClasses,
                    testKotlinClasses,
                    "build/tmp/kotlin-classes/debug",
                    "build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
                ) + configurations.getByName("debugRuntimeClasspath")
                mainClass.set(main)
            }
        }
        tasks.register("runAlgoTests") {
            group = "verification"
            dependsOn("runAlgoEngine", "runAlgoStats", "runAlgoFeatures")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    // 共享领域层（算法/模型/FHIR 导出），手机端与手表端共用，避免规则引擎重复实现
    implementation(project(":core"))

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.10.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
