plugins {
    id("arkikeskus.android.library")
    id("arkikeskus.android.hilt")
}

android {
    namespace = "org.arkikeskus.launcher.launcher"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
