plugins {
    id("relive.feature")
}

android {
    namespace = "com.jian.relive.feature.clean"
    resourcePrefix = "clean_"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:media"))

    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.material.icons.extended)
}
