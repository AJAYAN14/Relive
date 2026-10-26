plugins {
    id("relive.android.library")
    id("relive.compose.library")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", libs.findLibrary("androidx-navigation-compose").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
    add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
}
