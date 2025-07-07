
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.room)
}

fun generateVersionCode(year: Int, month: Int, day: Int): Int {
    return year * 10000 + month * 100 + day
}

android {
    namespace = "com.lovelive.dreamycolor"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lovelive.dreamycolor"
        minSdk = 26
        targetSdk = 36
        versionCode = generateVersionCode(2025, 4, 19)
        versionName = "0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // 只支持 arm64-v8a 和 x86_64
            abiFilters += setOf("arm64-v8a", "x86_64", "x86")
        }
    }

    room {
        schemaDirectory("$projectDir/schemas") // 指定数据库架构 schema 的导出目录
        // incremental = true // 增量处理通常由 KSP 自动处理，如果需要可以显式设置，但通常不需要
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false // 优化内存的神开关
            isShrinkResources = false // 优化内存的神开关2
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/CONTRIBUTORS.md"
            excludes += "/META-INF/LICENSE.md"
        }
    }
}


dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)


    // Compose UI 基础库
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation) // 基础布局、手势等
    implementation(libs.androidx.compose.animation) // 动画支持
    // Compose Material Design 3
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended) // Material 图标库
    // Compose 导航
    implementation(libs.androidx.navigation.compose)
    // Compose ViewModel 集成
    implementation(libs.androidx.lifecycle.viewmodel.compose) // 注意：libs.androidx.viewmodel.compose 可能是旧版或重复，建议统一使用 lifecycle-viewmodel-compose
    // Compose Paging 3 集成
    implementation(libs.androidx.paging.compose)
    // Compose Activity 集成
    implementation(libs.androidx.activity.compose)
    // Compose UI 工具 (预览、调试)
    implementation(libs.androidx.ui.tooling.preview) // 预览功能所需
    debugImplementation(libs.androidx.ui.tooling) // 调试工具，仅在 debug 构建中包含
    // --- AndroidX Core & Lifecycle ---
    implementation(libs.androidx.core.ktx) // Kotlin 扩展核心库
    implementation(libs.androidx.lifecycle.runtime.ktx) // Lifecycle 运行时 Kotlin 扩展
    // --- Persistence (数据持久化) ---
    // Room (数据库)
    implementation(libs.androidx.room.runtime) // Room 运行时
    implementation(libs.androidx.room.ktx) // Room Kotlin 扩展和协程支持
    ksp(libs.androidx.room.compiler) // Room 注解处理器 (KSP)
    // DataStore (键值对或类型化对象存储)
    implementation(libs.androidx.datastore) // DataStore 核心库 (可能不需要单独引入，看 preferences 是否包含)
    implementation(libs.androidx.datastore.preferences) // DataStore Preferences 实现
    // --- Background Processing (后台处理) ---
    implementation(libs.androidx.work.runtime.ktx) // WorkManager Kotlin 扩展和协程支持
    // --- Paging (分页加载) ---
    implementation(libs.androidx.paging.runtime) // Paging 3 运行时库
    // --- Image Loading (图片加载) ---
    implementation(libs.coil.compose) // Coil 图片加载库的 Compose 支持
    // --- JSON Serialization (JSON 序列化/反序列化) ---
    implementation(libs.gson) // Google Gson 库
    implementation(libs.kotlinx.serialization.json) // Kotlin 官方序列化库
    // --- Widgets (小组件) ---
    implementation(libs.androidx.glance.appwidget) // Glance AppWidget 支持
    // --- Utilities / Language Processing (工具库/语言处理) ---
    implementation(libs.kuromoji.ipadic) // Kuromoji 日语分词库 (带 IPADIC 词典)
    implementation(libs.pinyin4j) // Pinyin4j 汉字转拼音库
    // --- Legacy View System / Interop (旧视图系统/互操作) ---
    // 如果你在 Compose 中使用了 AndroidView 来嵌入旧版 View，可能会需要
    implementation(libs.androidx.viewpager2) // ViewPager2 支持
    // --- Unit Testing (单元测试) ---
    testImplementation(libs.junit) // JUnit 4 测试框架
    // --- Android Instrumentation Testing (Android 设备/模拟器测试) ---
    androidTestImplementation(libs.androidx.junit) // AndroidX Test 扩展 for JUnit
    androidTestImplementation(libs.androidx.espresso.core) // Espresso UI 测试核心库
    androidTestImplementation(libs.androidx.ui.test.junit4) // Compose UI 测试工具 for JUnit4
    debugImplementation(libs.androidx.ui.test.manifest) // Compose UI 测试 Manifest 工具
}











