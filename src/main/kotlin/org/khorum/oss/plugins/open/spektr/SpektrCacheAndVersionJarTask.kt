package org.khorum.oss.plugins.open.spektr

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.security.MessageDigest

abstract class SpektrCacheAndVersionJarTask : DefaultTask() {

    @get:InputFile
    abstract val jarFile: RegularFileProperty

    @get:Input
    abstract val jarBaseName: Property<String>

    @get:Input
    abstract val currentVersion: Property<String>

    @get:InputFile
    abstract val versionFile: RegularFileProperty

    @get:OutputDirectory
    abstract val dockerJarsDir: DirectoryProperty

    init {
        group = "spektr"
    }

    @TaskAction
    fun execute() {
        val builtJar = jarFile.get().asFile
        val outputDir = dockerJarsDir.get().asFile
        val baseName = jarBaseName.get()
        val hashFile = File(outputDir, "$baseName.sha256")

        val newHash = MessageDigest.getInstance("SHA-256")
            .digest(builtJar.readBytes())
            .joinToString("") { "%02x".format(it) }

        val previousHash = if (hashFile.exists()) hashFile.readText().trim() else ""

        // Check if any JAR for this module already exists in the output dir
        val existingJar = outputDir.listFiles()?.any {
            it.name.startsWith("$baseName-") && it.name.endsWith(".jar")
        } ?: false

        when {
            // No JAR present at all — deploy current version without bumping
            !existingJar -> {
                logger.lifecycle("No existing JAR found for $baseName — adding ${currentVersion.get()}")
                builtJar.copyTo(File(outputDir, builtJar.name), overwrite = true)
                hashFile.writeText(newHash)
            }

            // JAR exists but content changed — clean old, copy new, bump version
            newHash != previousHash -> {
                logger.lifecycle("Changes detected for $baseName — bumping version")

                outputDir.listFiles()?.filter {
                    it.name.startsWith("$baseName-") && it.name.endsWith(".jar")
                }?.forEach { it.delete() }

                val vFile = versionFile.get().asFile
                val parts = currentVersion.get().split(".").toMutableList()
                parts[2] = (parts[2].toInt() + 1).toString()
                val newVersion = parts.joinToString(".")
                vFile.writeText(newVersion)
                logger.lifecycle("Version incremented: ${currentVersion.get()} -> $newVersion")

                // Rebuild filename with new version
                val newJarName = "$baseName-$newVersion.jar"
                builtJar.copyTo(File(outputDir, newJarName), overwrite = true)
                logger.lifecycle("Copied $newJarName to ${outputDir.path}")

                hashFile.writeText(newHash)
            }

            // JAR exists, no changes
            else -> {
                logger.lifecycle("No changes detected for $baseName — skipping")
            }
        }
    }
}