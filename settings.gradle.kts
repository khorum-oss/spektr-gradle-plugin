rootProject.name = "spektr-gradle-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            url = uri("https://open-reliquary.nyc3.cdn.digitaloceanspaces.com")
        }
    }
}