plugins {
    id("relive.android.library")
}

android {
    namespace = "com.jian.relive.core.common"
    resourcePrefix = "common_"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.gson)
    implementation(libs.androidx.core.ktx)
}
