plugins {
    id("relive.android.library")
    id("relive.compose.library")
}

android {
    namespace = "com.jian.relive.core.ui"
    resourcePrefix = "ui_"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
}
