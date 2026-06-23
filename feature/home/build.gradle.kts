plugins {
    id("arkikeskus.android.feature")
}

android {
    namespace = "org.arkikeskus.launcher.feature.home"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:launcher"))
    implementation(project(":core:model"))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
}
