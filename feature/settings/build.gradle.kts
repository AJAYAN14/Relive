plugins {
    id("relive.feature")
}

android {
    namespace = "com.jian.relive.feature.settings"
    resourcePrefix = "settings_"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
}
