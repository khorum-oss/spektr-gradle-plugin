# Spektr Gradle Plugin

A Gradle plugin for building and versioning [Spektr](https://github.com/khorum-oss/spektr) endpoint JARs. It wraps the Shadow plugin to produce fat JARs with DSL classes excluded, and provides automatic version bumping and caching for Docker-based workflows.

## Requirements

- Gradle 8.0+
- Java 21+

## Features

- **Shadow JAR packaging** - Produces a fat JAR with all dependencies, excluding Spektr DSL classes (which are provided by the server at runtime)
- **Automatic version management** - Reads the version from a file and auto-increments the patch version when JAR contents change
- **Docker JAR caching** - Copies versioned JARs to a configurable directory (e.g. `docker/jars`) and skips unchanged builds via SHA-256 hash comparison
- **Service file merging** - Merges `META-INF/services` files so `EndpointModule` implementations are discovered correctly

## Installation

Add the Khorum OSS repository to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://open-reliquary.nyc3.cdn.digitaloceanspaces.com")
        }
    }
}
```

Apply the plugin in your module's `build.gradle.kts`:

```kotlin
plugins {
    id("org.khorum.oss.plugins.open.spektr") version "<latest>"
}
```

The Spektr DSL dependency should be added as `compileOnly` since the server provides it at runtime:

```kotlin
dependencies {
    compileOnly("org.khorum.oss:spektr-dsl:<version>")
}
```

## Configuration

The plugin adds a `spektr` extension with an `apiProvider` block for configuring how the endpoint JAR is built and versioned:

```kotlin
spektr {
    apiProvider {
        jarBaseName = "my-endpoints"    // Base name for the output JAR (required)
        versionFile = "version.txt"     // File containing the current version (default: "version.txt")
        dockerJarsDir = "docker/jars"   // Output directory for cached JARs (default: "docker/jars")
    }
}
```

Create a version file (default `version.txt`) in your module directory containing a semver string:

```
1.0.0
```

> **Note:** `dockerJarsDir` is resolved relative to the **root project** directory, not the subproject. This allows multiple modules in a multi-module build to output JARs to the same shared directory.

## Tasks

| Task | Description |
|------|-------------|
| `shadowJar` | Builds the fat JAR with Spektr DSL classes excluded. Configured by the plugin, provided by [Shadow](https://github.com/GradleUp/shadow). |
| `cacheAndVersionJar` | Copies the JAR to the Docker jars directory, bumping the patch version if contents changed. Registered by this plugin. |

### Usage

```shell
# Build the fat JAR
./gradlew shadowJar

# Build, version, and cache the JAR for Docker
./gradlew cacheAndVersionJar
```

## How Versioning Works

The `cacheAndVersionJar` task uses SHA-256 hashing to detect changes:

1. **No existing JAR** - Copies the built JAR to the output directory as-is
2. **JAR exists but contents changed** - Increments the patch version in the version file, removes the old JAR, and copies the new one
3. **No changes** - Skips the copy entirely

## License

MIT
