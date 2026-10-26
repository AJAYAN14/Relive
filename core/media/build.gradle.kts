plugins {
    id("relive.android.library")
}

android {
    namespace = "com.jian.relive.core.media"
    resourcePrefix = "media_"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.androidx.exifinterface)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    implementation(libs.media3.transformer)
    implementation(libs.mlkit.image.labeling)
    implementation(libs.mlkit.face.detection)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.gson)
}
