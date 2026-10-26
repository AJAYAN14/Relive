import com.android.build.api.dsl.LibraryExtension

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<LibraryExtension> {
    buildFeatures {
        compose = true
    }
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    val bom = libs.findLibrary("androidx-compose-bom").get()
    add("implementation", platform(bom))
    add("implementation", libs.findLibrary("androidx-compose-ui").get())
    add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
    add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
    add("implementation", libs.findLibrary("androidx-compose-material3").get())
    add("implementation", libs.findLibrary("androidx-compose-material-icons-extended").get())
    add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
}
