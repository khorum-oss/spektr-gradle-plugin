import org.khorum.oss.plugins.open.publishing.digitalocean.domain.uploadToDigitalOceanSpaces
import org.khorum.oss.plugins.open.publishing.mavengenerated.domain.mavenGeneratedArtifacts
import org.khorum.oss.plugins.open.secrets.getPropertyOrEnv
import kotlin.apply

plugins {
	kotlin("jvm") version "2.3.0"
	id("dev.detekt") version "2.0.0-alpha.2"
	id("org.jetbrains.dokka") version "2.1.0"
	id("org.jetbrains.dokka-javadoc") version "2.1.0"
	id("org.jetbrains.kotlinx.kover") version "0.7.6"
	id("org.khorum.oss.plugins.open.publishing.maven-generated-artifacts") version "1.0.0"
	id("org.khorum.oss.plugins.open.publishing.digital-ocean-spaces") version "1.0.0"
	id("org.khorum.oss.plugins.open.secrets") version "1.0.0"
	id("org.khorum.oss.plugins.open.pipeline") version "1.0.0"
	`kotlin-dsl`
}

group = "org.khorum.oss.plugins.open.spektr"
version = file("VERSION").readText().trim()


buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath(kotlin("gradle-plugin", version = "2.0.20"))
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("io.github.microutils:kotlin-logging:4.0.0-beta-2")

	implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")

	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	testImplementation("io.mockk:mockk:1.13.8") // For mocking in Kotlin
	testImplementation(gradleTestKit())
}

tasks.jar {
	archiveBaseName.set("spektr")
}

// Dokka V2's `dokkaHtml` is a lifecycle task that errors by default. Wire it to the V2 generate task.
tasks.named("dokkaHtml") {
	dependsOn("dokkaGenerateHtml")
}

// Disable Kover instrumentation globally to avoid race condition
// with kover-agent.args file during parallel builds (Kover 0.7.x bug)
extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
	disable()
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
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
		connection.set("https://github.com/khorum-oss/spektr-gradle-plugin.git")
	}
}

detekt {
	buildUponDefaultConfig = true
	allRules = false
	config.setFrom(files("$rootDir/detekt.yml"))
	source.setFrom("src/main/kotlin")
	parallel = true
}
