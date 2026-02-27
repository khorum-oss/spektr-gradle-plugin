package org.khorum.oss.plugins.open.spektr

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

private const val EXCLUDE_PACKAGES = "org/khorum/oss/spektr/dsl/**"

class SpektrPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = target.run {
        plugins.apply("com.gradleup.shadow")
        val ext = extensions.create<SpektrJarLoadPreProcessingExtension>("spektr")

        afterEvaluate {
            // Read version from the version file after the extension is configured
            val versionFromFile = file(ext.apiProvider().versionFile).readText().trim()
            ext.apiProvider().version = versionFromFile
        }

        val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
            archiveClassifier.set("")
            archiveFileName.set(provider { ext.apiProvider().toJarName() })
            exclude(EXCLUDE_PACKAGES)
            mergeServiceFiles()
        }

        tasks.register<SpektrCacheAndVersionJarTask>("cacheAndVersionJar") {
            val baseName = requireNotNull(ext.apiProvider().jarBaseName) { "jarBaseName must be set" }

            jarFile.set(shadowJarTask.flatMap { it.archiveFile })
            jarBaseName.set(baseName)
            versionFile.set(layout.projectDirectory.file(ext.apiProvider().versionFile))
            currentVersion.set(versionFile.asFile.get().readText().trim())
            dockerJarsDir.set(rootProject.layout.projectDirectory.dir(ext.apiProvider().dockerJarsDir))

            dependsOn(shadowJarTask)
        }
    }
}
