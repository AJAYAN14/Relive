plugins {
    `kotlin-dsl`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation("com.android.tools.build:gradle:${libs.findVersion("agp").get().requiredVersion}")
    implementation("org.jetbrains.kotlin:compose-compiler-gradle-plugin:${libs.findVersion("kotlin").get().requiredVersion}")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.findVersion("kotlin").get().requiredVersion}")
    implementation("org.jetbrains.kotlin:kotlin-serialization:${libs.findVersion("kotlin").get().requiredVersion}")
}
