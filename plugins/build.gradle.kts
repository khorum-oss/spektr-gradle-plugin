import org.khorum.oss.plugins.open.publishing.digitalocean.domain.uploadToDigitalOceanSpaces
import org.khorum.oss.plugins.open.publishing.mavengenerated.domain.mavenGeneratedArtifacts
import org.khorum.oss.plugins.open.secrets.getPropertyOrEnv

plugins {
    `kotlin-dsl`
    id("org.jetbrains.dokka")
    id("org.khorum.oss.plugins.open.publishing.maven-generated-artifacts")
    id("org.khorum.oss.plugins.open.publishing.digital-ocean-spaces")
}

group = "org.khorum.oss.plugins.open"

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", version = "2.0.20"))
    }
}


tasks.jar {
    archiveBaseName.set("spektr")
}

repositories {
    // Add any required repositories
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.13.8") // For mocking in Kotlin
    testImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("spektrPlugin") {
            id = "org.khorum.oss.plugins.open.spektr"
            version = version.toString()
            implementationClass = "org.khorum.oss.plugins.open.spektr.SpektrPlugin"
        }
    }
}

digitalOceanSpacesPublishing {
    bucket = "open-reliquary"
    accessKey = project.getPropertyOrEnv("spaces.key", "DO_SPACES_API_KEY")
    secretKey = project.getPropertyOrEnv("spaces.secret", "DO_SPACES_SECRET")
    publishedVersion = version.toString()
    isPlugin = true
    dryRun = false
}

tasks.uploadToDigitalOceanSpaces?.apply {
    val task: Task = tasks.mavenGeneratedArtifacts ?: throw Exception("mavenGeneratedArtifacts task not found")
    dependsOn(task)
}

mavenGeneratedArtifacts {
    publicationName = "digitalOceanSpaces"  // Must match the name expected by the DO Spaces plugin
    name = "Spektr Plugin"
    description = """
            A plugin for configuring Spektr endpoints in Gradle projects.
        """
    websiteUrl = "https://github.com/khorum-oss/spektr/tree/main/plugins"

    licenses {
        license {
            name = "MIT License"
            url = "https://opensource.org/license/mit"
        }
    }

    developers {
        developer {
            id = "khorum-oss"
            name = "Khorum OSS Team"
            email = "khorum.oss@gmail.com"
            organization = "Khorum OSS"
        }
    }

    scm {
        connection.set("https://github.com/khorum-oss/spektr.git")
    }
}