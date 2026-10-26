plugins {
    id("relive.feature")
}

android {
    namespace = "com.jian.relive.feature.feed"
    resourcePrefix = "feed_"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:media"))

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.common)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
}
