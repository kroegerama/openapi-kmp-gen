![KMP Targets](https://img.shields.io/badge/kmp%20targets-JVM%20Android%20iOS%20macOS%20Windows%20Linux-blue?style=flat)
![API Level](https://img.shields.io/badge/min%20sdk-API%2021-blue?style=flat)

![GitHub Actions Workflow Status](https://img.shields.io/github/actions/workflow/status/kroegerama/openapi-kmp-gen/gradle.yml?style=flat)
[![Maven Central Version](https://img.shields.io/maven-central/v/com.kroegerama.openapi-kmp-gen/companion?style=flat)](https://central.sonatype.com/search?namespace=com.kroegerama.openapi-kmp-gen)

# OpenAPI KMP Gen

This gradle plugin, the CLI (and companion library) are the successor of the jvm/android only
plugin [OpenAPI KGen](https://github.com/kroegerama/openapi-kgen).
It comes with support for KMP and the targets JVM, Android, iOS, macOS, Windows and Linux.

## Features

- Kotlin Multiplatform
- Coroutines
- Either responses
- HTTP Status responses
- Json support via kotlinx-serialization
- Date types via kotlinx-datetime
- Http calls via ktor
- Supports security
- Generated named primitives
- Allows injection of decorators for ktor client, serialization, etc.
- Allows filtering of APIs to only generate a subset of the OpenAPI using tags

### Example

Using the [testspec.yaml](generated/testspec.yaml) file, the following code will be generated:
[generated/src](generated/src/commonMain/kotlin/com/kroegerama/kmp/gen/generated)

## Technologies used

### Generator

- [Kotlin Poet](https://github.com/square/kotlinpoet)
- [Airlift Airline](https://github.com/airlift/airline)
- [Swagger Parser](https://github.com/swagger-api/swagger-parser)

### Generated Code

- [Ktor](https://ktor.io/)
- [Arrow](https://arrow-kt.io/)
- [KotlinX Serialization](https://kotlinlang.org/docs/serialization.html)
- [KotlinX DateTime](https://github.com/Kotlin/kotlinx-datetime)

## Gradle plugin

The plugin can be used by any JVM, Android or KMP project.

Modify your `libs.versions.toml`, add

```toml
[versions]
kmpgen = "<latest.version>"
# ...

[plugins]
kmpgen = { id = "com.kroegerama.openapi-kmp-gen", version.ref = "kmpgen" }
# ...
```

Register the plugin in your toplevel `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kmpgen) apply false
    // ...
}
```

Add the plugin in your module `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kmpgen)
    // ...
}

// ...

kmpgen {
    spec(
        packageName = "com.myproject.api"
    ) {
        specFile = file("my-spec.yaml")
    }
    /* if you want to generate multiple specs
    spec(
        packageName = "com.myproject.api2"
    ) {
        specFile = file("my-spec-2.yaml")
    }
     */
}
```

## Companion library

The companion library is automatically added as a dependency, when the plugin is added.
It centralizes the most important building blocks, all generated apis need.
The library can be used by any project (KMP or non-KMP).

```toml
[versions]
kmpgen = "<latest.version>"
# ...

[libraries]
kmpgen-companion = { module = "com.kroegerama.openapi-kmp-gen:companion", version.ref = "kmpgen" }
# ...
```

## CLI

If you just want to generate code without using a gradle plugin, the CLI can be used:

```
NAME
        openapi-kmpgen generate - Generate code from the specified OpenAPI Spec.

SYNOPSIS
        openapi-kmpgen generate [(-a | --generate-all-named-schemas)]
                [--allow-parse-errors] [(-l <limit apis> | --limit-apis <limit apis>)]
                (-o <output directory> | --output <output directory>)
                [(-p <package name> | --package-name <package name>)]
                [(-s | --output-src)] [(-v | --verbose)] [--] <spec file>

OPTIONS
        -a, --generate-all-named-schemas


        --allow-parse-errors
            Try to generate classes, even if parsing errors occur in the spec.

        -l <limit apis>, --limit-apis <limit apis>
            If set, generate only these APIs (set via tag) and their models.
            Comma separated list. Example: "auth,app"

        -o <output directory>, --output <output directory>


        -p <package name>, --package-name <package name>


        -s, --output-src


        -v, --verbose


        --
            This option can be used to separate command-line options from the
            list of argument, (useful when arguments might be mistaken for
            command-line options

        <spec file>
            Spec file (yaml/json). Can be a file or url.
```

## License

```
Copyright 2020 kroegerama

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
