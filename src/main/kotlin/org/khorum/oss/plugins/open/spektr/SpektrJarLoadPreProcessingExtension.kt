package org.khorum.oss.plugins.open.spektr

open class SpektrJarLoadPreProcessingExtension {
    private val apiProvider: ApiProvider = ApiProvider()

    internal fun apiProvider(): ApiProvider = apiProvider

    fun apiProvider(action: ApiProvider.() -> Unit) {
        action(apiProvider)
    }

    class ApiProvider {
        var jarBaseName: String? = null
        var version: String? = null
        var versionFile: String = "version.txt"
        var dockerJarsDir: String = "docker/jars"

        internal fun toJarName(): String = "$jarBaseName-$version.jar"
    }
}