plugins {
    id("relive.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.jian.relive.core.model"
    resourcePrefix = "model_"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
}
