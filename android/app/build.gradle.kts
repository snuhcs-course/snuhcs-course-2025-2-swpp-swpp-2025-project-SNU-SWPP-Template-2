plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
    id("jacoco")
}

android {
    namespace = "com.example.mindlog"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.mindlog"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.mindlog.HiltTestRunner"

        buildConfigField("String", "API_BASE_URL", "\"http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/api/v1/\"")
        buildConfigField("String", "S3_BUCKET_URL", "\"https://mindlog-s3.s3.ap-northeast-2.amazonaws.com\"")
    }

    // 🔹 추가: Signing configuration
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "my-release-key.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/api/v1/\"")
            buildConfigField("String", "S3_BUCKET_URL", "\"https://mindlog-s3.s3.ap-northeast-2.amazonaws.com\"")
            // buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/api/v1/\"")

            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/api/v1/\"")
            buildConfigField("String", "S3_BUCKET_URL", "\"https://mindlog-s3.s3.ap-northeast-2.amazonaws.com\"")
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release") // 🔹 추가
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // ---- Runtime (app) ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.tbuonomo:dotsindicator:4.3")
    implementation("com.airbnb.android:lottie:6.0.0")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // SavedState 추가 (Fragment / Nav 최신 버전 호환)
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    androidTestImplementation("androidx.savedstate:savedstate-ktx:1.2.1")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Dependency for visualization
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.patrykandpatrick.vico:views:1.14.0")
    implementation("com.patrykandpatrick.vico:compose:1.14.0")
    implementation("com.github.jolenechong:androidWordCloud:1.0.0") {
        exclude(group="com.sun.xml.bind", module="jaxb-core")
        exclude(group="com.sun.xml.bind", module="jaxb-impl")
    }

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")

    // Hilt Navigation
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // ---- Unit Test (Local JVM) ----
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")

    // ---- Instrumented Test + UI (androidTest) ----
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    androidTestImplementation("com.google.truth:truth:1.4.4")

    // Hilt Testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.52")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.52")

    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.espresso.contrib)

    // Network Integration Test
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    testImplementation(kotlin("test"))

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("com.google.truth:truth:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
}

kapt {
    correctErrorTypes = true
}

configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    }
}

jacoco {
    toolVersion = "0.8.12"   // 또는 최신 권장 버전
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    // Ensure exec file is generated with Android unit tests (Robolectric, inline, etc.)
    extensions.configure(org.gradle.testing.jacoco.plugins.JacocoTaskExtension::class) {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register("unitTestCoverageReport", org.gradle.testing.jacoco.tasks.JacocoReport::class.java) {
    dependsOn("testDebugUnitTest")

    val excludes = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Hilt*.*", "**/*_HiltModules*.*", "**/hilt_aggregated_deps/**",
        "**/databinding/**", "**/BR.class"
    )

    val kotlinClassesDir = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
    val javaClassesDir = layout.buildDirectory.dir("intermediates/javac/debug/classes").get().asFile
    classDirectories.setFrom(
        files(
            fileTree(kotlinClassesDir) { exclude(excludes) },
            fileTree(javaClassesDir) { exclude(excludes) }
        )
    )

    executionData.setFrom(
        fileTree(layout.buildDirectory.asFile.get()) {
            include(
                "jacoco/testDebugUnitTest.exec",                               // classic
                "outputs/unit_test_code_coverage/**/testDebugUnitTest.exec",   // AGP 7.x/8.x
                "outputs/unit_test_code_coverage/**/*.exec"                    // fallback
            )
        }
    )

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    reports {
        html.required.set(true)
        xml.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/unitTest"))
    }
}

tasks.register("printUnitCoverageExecs") {
    dependsOn("testDebugUnitTest")
    doLast {
        val tree = fileTree(layout.buildDirectory.asFile.get()) {
            include(
                "jacoco/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/**/testDebugUnitTest.exec",
                "outputs/unit_test_code_coverage/**/*.exec"
            )
        }
        println("\n[unit coverage] exec files found:\n" + tree.files.joinToString("\n") + "\n")
    }
}

// ---- UI(androidTest) coverage helpers ----
tasks.register("printAndroidCoverageEcs") {
    dependsOn("connectedDebugAndroidTest")
    doLast {
        val tree = fileTree(layout.buildDirectory.asFile.get()) {
            include(
                "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                "outputs/code_coverage/**/*.ec"
            )
        }
        println("\n[androidTest coverage] .ec files found:\n" + tree.files.joinToString("\n") + "\n")
    }
}

// ---- Final merged HTML/XML report ----
tasks.register("debugCoverageReport", org.gradle.testing.jacoco.tasks.JacocoReport::class.java) {
    dependsOn("testDebugUnitTest", "connectedDebugAndroidTest")

    val excludes = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Hilt*.*", "**/*_HiltModules*.*", "**/hilt_aggregated_deps/**",
        "**/databinding/**", "**/BR.class"
    )

    val kotlinClassesDir = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile
    val javaClassesDir = layout.buildDirectory.dir("intermediates/javac/debug/classes").get().asFile
    classDirectories.setFrom(
        files(
            fileTree(kotlinClassesDir) { exclude(excludes) },
            fileTree(javaClassesDir) { exclude(excludes) }
        )
    )

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        files(
            // unit test exec candidates
            fileTree(layout.buildDirectory.asFile.get()) {
                include(
                    "jacoco/testDebugUnitTest.exec",
                    "outputs/unit_test_code_coverage/**/testDebugUnitTest.exec",
                    "outputs/unit_test_code_coverage/**/*.exec"
                )
            },
            // androidTest ec candidates
            fileTree(layout.buildDirectory.asFile.get()) {
                include(
                    "outputs/code_coverage/debugAndroidTest/connected/**/*.ec",
                    "outputs/code_coverage/**/*.ec"
                )
            }
        )
    )

    reports {
        html.required.set(true)
        xml.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/mergedDebug"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/mergedDebug.xml"))
    }
}
// ---- Convenience: run all tests and generate merged coverage in one go ----
tasks.register("coverageAll") {
    // Run unit tests -> run androidTest -> build merged coverage report
    dependsOn(
        "testDebugUnitTest",
        "connectedDebugAndroidTest",
        "debugCoverageReport"
    )
    doLast {
        println(
            "\n✅ Coverage reports generated:" +
                "\n - Unit:      ${layout.buildDirectory.dir("reports/jacoco/unitTest").get().asFile}/index.html" +
                "\n - Merged(UI+Unit): ${layout.buildDirectory.dir("reports/jacoco/mergedDebug").get().asFile}/index.html\n"
        )
    }
}

// (Optional) Unit-only quick coverage
tasks.register("coverageUnitOnly") {
    dependsOn("testDebugUnitTest", "unitTestCoverageReport")
    doLast {
        println(
            "\n✅ Unit coverage report:" +
                "\n - ${layout.buildDirectory.dir("reports/jacoco/unitTest").get().asFile}/index.html\n"
        )
    }
}