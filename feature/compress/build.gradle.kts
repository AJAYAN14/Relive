plugins {
    id("relive.feature")
}

android {
    namespace = "com.jian.relive.feature.compress"
    resourcePrefix = "compress_"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:media"))

    implementation(libs.androidx.exifinterface)
    implementation(libs.media3.transformer)
    implementation(libs.media3.common)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
}

