import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// 构建编号：每执行一次 assemble 编译导出自动 +1（IDE 同步等非编译调用不计）
// 编号写入 versionCode / versionName，产物文件名随之变化，如 Sisyphus-1.0.3-debug.apk
val counterProps = Properties()
val counterFile = rootProject.file("build-counter.properties")
if (counterFile.exists()) {
    counterFile.inputStream().use { counterProps.load(it) }
}
val isCompileExport = gradle.startParameter.taskNames.any {
    it.contains("assemble", ignoreCase = true) || it.contains("bundle", ignoreCase = true)
}

// 签名配置（keystore.properties 保存密钥信息，不入库）
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}
var buildCount = counterProps.getProperty("buildCount")?.toIntOrNull() ?: 0
if (isCompileExport) {
    buildCount += 1
    counterProps.setProperty("buildCount", buildCount.toString())
    counterFile.outputStream().use { counterProps.store(it, "auto build counter") }
}

// 版本号：patch 每满 100 进位到 minor（...2.0.100 → 2.1.1 → 2.1.26）
val versionMinor = (buildCount - 1).coerceAtLeast(0) / 100
val versionPatch = (buildCount - 1).coerceAtLeast(0) % 100 + 1
val versionNameFull = "2.$versionMinor.$versionPatch"

android {
    namespace = "cn.wangce.lumi"
    compileSdk = 35

    defaultConfig {
        applicationId = "cn.wangce.lumi"
        minSdk = 26
        targetSdk = 35
        versionCode = buildCount
        versionName = versionNameFull
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 音乐上传 GitHub Token：从环境变量或 ~/.gradle/gradle.properties 注入，勿硬编码
        buildConfigField("String", "GH_TOKEN", "\"${project.findProperty("GH_TOKEN") ?: ""}\"")
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePropsFile.exists()) signingConfigs.getByName("release") else null
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // 产物文件名：Sisyphus-<versionName>.apk（含自动构建编号）
    applicationVariants.all {
        outputs.all {
            (this as BaseVariantOutputImpl).outputFileName = "Sisyphus-${versionName}.apk"
        }
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // SceneView (3D 渲染引擎，基于 Google Filament)
    implementation("io.github.sceneview:sceneview:2.3.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
