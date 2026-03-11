# ResChiper

<p align="center">
  <img src="artifacts/reschiper-banner.png" alt="ResChiper banner" />
</p>

<p align="center">Gradle plugin for Android App Bundle resource obfuscation, duplicate resource merging, file filtering, and string cleanup.</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache_2.0-maroon" alt="License"></a>
  <img src="https://img.shields.io/badge/JDK-17-blue" alt="JDK 17">
  <img src="https://img.shields.io/badge/Bundletool-1.17.2-red" alt="Bundletool 1.17.2">
  <img src="https://img.shields.io/badge/release-0.1.0--rc6-%23C6782A.svg" alt="Release 0.1.0-rc6">
</p>

## Overview

ResChiper post-processes an Android `.aab` after the normal bundle task runs. It can:

- obfuscate resource file and directory names
- reuse a previous mapping file for stable obfuscation output
- merge duplicate bundled resources to reduce size
- filter selected files from `META-INF/` and `lib/`
- remove unused string values and non-whitelisted locales

The plugin only supports Android application modules and works on App Bundles, not APKs.

## Requirements

- JDK 17
- Android Gradle Plugin 8.x
- An Android app module using `com.android.application`
- A build that produces an `.aab` for the target variant

## Installation

Published artifact:

```text
io.github.goldfish07.reschiper:plugin:<version>
```

Add the dependency in the root build script:

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("io.github.goldfish07.reschiper:plugin:0.1.0-rc6")
    }
}
```

Apply the plugin in the Android application module:

```kotlin
plugins {
    id("com.android.application")
}

apply(plugin = "io.github.goldfish07.reschiper")
```

Groovy DSL:

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "io.github.goldfish07.reschiper:plugin:0.1.0-rc6"
    }
}

apply plugin: "com.android.application"
apply plugin: "io.github.goldfish07.reschiper"
```

## Quick Start

Minimal Kotlin DSL configuration:

```kotlin
configure<io.github.goldfish07.reschiper.plugin.Extension> {
    enableObfuscation = true
    obfuscationMode = "default"
    obfuscatedBundleName = "app-obfuscated.aab"
    whiteList = setOf("*.R.string.app_name")
}
```

Minimal Groovy DSL configuration:

```groovy
resChiper {
    enableObfuscation = true
    obfuscationMode = "default"
    obfuscatedBundleName = "app-obfuscated.aab"
    whiteList = [
        "*.R.string.app_name"
    ]
}
```

Run the regular bundle task for a variant:

```bash
./gradlew :app:bundleRelease --stacktrace
```

ResChiper creates variant-specific tasks named `resChiper<Variant>`, so you can also run:

```bash
./gradlew :app:resChiperRelease --stacktrace
```

## How It Fits Into the Build

For each application variant, the plugin creates a `resChiper<Variant>` task and wires it into `bundle<Variant>`.

Typical flow:

1. Android packaging/signing tasks produce the source `.aab`.
2. ResChiper reads that bundle.
3. Optional filters and duplicate merging run.
4. Resource obfuscation runs.
5. A new obfuscated `.aab` is written beside the original bundle output.

## Configuration Reference

| Property | Type | Default | Notes |
| --- | --- | --- | --- |
| `enableObfuscation` | `boolean` | `true` | Enables resource obfuscation. |
| `obfuscationMode` | `String` | `"default"` | Supported values: `default`, `dir`, `file`. |
| `obfuscatedBundleName` | `String` | none | Required output file name for the rewritten bundle. |
| `mappingFile` | `Path` | `null` | Reuses an existing `resources-mapping.txt` for stable naming. |
| `whiteList` | `Set<String>` | empty | Excludes matching resources or paths from obfuscation. |
| `mergeDuplicateResources` | `boolean` | `false` | Merges duplicate bundled resources and emits a duplicate log. |
| `enableFileFiltering` | `boolean` | `false` | Enables bundle file filtering. |
| `fileFilterList` | `Set<String>` | empty | Supports filtering within `META-INF/` and `lib/`. |
| `enableFilterStrings` | `boolean` | `false` | Removes unused strings and optionally filters locales. |
| `unusedStringFile` | `String` | `""` | Path to a newline-delimited `unused_strings.txt`. |
| `localeWhiteList` | `Set<String>` | empty | Keeps only listed locales, such as `en`, `fr`, `in`. |

## Whitelist Rules

`whiteList` accepts glob-style rules. Common patterns:

```kotlin
whiteList = setOf(
    "res/raw",
    "res/raw/*",
    "res/xml/*",
    "*.R.raw.*",
    "*.R.xml.*",
    "*.R.string.google_app_id",
    "*.R.string.app_name"
)
```

Use whitelist rules for:

- resource directories or files that must keep stable names
- generated resources from Google services or Firebase
- assets referenced by external systems or dynamic loaders

## Output Files

ResChiper writes files into the same bundle output directory as the original `.aab`.

- obfuscated bundle: the file named by `obfuscatedBundleName`
- `resources-mapping.txt`: resource name mapping for incremental reuse
- `<module>-duplicate.txt`: duplicate resource report when duplicate merging is enabled

## Sample Projects

This repository includes two runnable sample apps:

- [sample-app](sample-app) for Kotlin DSL
- [sample-app-groovy](sample-app-groovy) for Groovy DSL

They are configured for local plugin development with a composite build, so you can test the plugin without publishing it first.

Example commands:

```bash
./gradlew -p sample-app bundleDebug --stacktrace
./gradlew -p sample-app-groovy bundleDebug --stacktrace
```

## Notes and Limitations

- ResChiper requires `com.android.application`; library modules are rejected.
- The plugin is designed around `.aab` processing.
- If you enable file filtering, only `META-INF/` and `lib/` entries are supported by the implementation.
- If you enable string filtering, provide `unusedStringFile` unless your build already produces an `unused_strings.txt` report that ResChiper can reuse.

## Acknowledgments

- [AabResGuard](https://github.com/bytedance/AabResGuard/)
- [AndResGuard](https://github.com/shwenzhang/AndResGuard/)
- [Bundletool](https://github.com/google/bundletool)

## License

ResChiper is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
