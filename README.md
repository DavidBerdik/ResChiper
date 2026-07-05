# ResChiper

<p align="center">
  <img src="artifacts/reschiper-banner.png" alt="ResChiper banner" />
</p>

<p align="center">Gradle plugin for Android App Bundle resource obfuscation, duplicate resource merging, file filtering, and string cleanup.</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-Apache_2.0-maroon" alt="License"></a>
  <img src="https://img.shields.io/badge/JDK-17-blue" alt="JDK 17">
  <img src="https://img.shields.io/badge/Bundletool-1.17.2-red" alt="Bundletool 1.17.2">
  <img src="https://img.shields.io/badge/release-0.1.0--rc7.2-%23C6782A.svg" alt="Release 0.1.0-rc7.2">
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

Recommended Kotlin DSL usage:

```kotlin
plugins {
    id("com.android.application")
    id("io.github.goldfish07.reschiper") version "0.1.0-rc7.2"
}
```

Legacy classpath usage is also supported. Add the dependency in the root build script:

```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("io.github.goldfish07.reschiper:plugin:0.1.0-rc7.2")
    }
}
```

Then apply the plugin in the Android application module:

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
        classpath "io.github.goldfish07.reschiper:plugin:0.1.0-rc7.2"
    }
}

apply plugin: "com.android.application"
apply plugin: "io.github.goldfish07.reschiper"
```

## Quick Start

Full Kotlin DSL configuration:

```kotlin
configure<io.github.goldfish07.reschiper.plugin.Extension> {
    enableObfuscation = true // by default res obfuscate is enabled
    obfuscationMode = "default" // ["dir", "file", "default"]
    obfuscatedBundleName = "reschiper-app.aab" // Obfuscated file name, must end with ".aab"
    // mappingFile = file("path/to/your/mapping.txt").toPath() // Mapping file used for incremental obfuscation
    whiteList = setOf(
        // Whitelist rule (directory name to exclude)
        "res/raw", // raw dir will not be obfuscated
        "res/xml", // xml dir will not be obfuscated

        // Whitelist rule (file name to exclude)
        "res/raw/*", // all files inside raw directory will not be obfuscated
        "res/raw/success_tick.json", // success_tick.json file will not be obfuscated
        "res/xml/*", // all files inside xml directory will not be obfuscated

        // Whitelist rules (resource name to exclude)
        "*.R.raw.*",
        "*.R.xml.*",

        // for google-services
        "*.R.string.google_api_key",
        "*.R.string.google_app_id",
        "*.R.string.default_web_client_id",
        "*.R.string.gcm_defaultSenderId",
        "*.R.string.ga_trackingId",
        "*.R.string.firebase_database_url",
        "*.R.string.google_crash_reporting_api_key",
        "*.R.string.google_storage_bucket",
        "*.R.integer.google_play_services_version",

        // firebase
        "*.R.string.project_id",
        // firebase crashlytics
        "*.R.string.com.google.firebase.crashlytics.mapping_file_id",
        "*.R.bool.com.crashlytics.useFirebaseAppId",
        "*.R.string.com.crashlytics.useFirebaseAppId",
        "*.R.string.google_app_id",
        "*.R.bool.com.crashlytics.CollectDeviceIdentifiers",
        "*.R.string.com.crashlytics.CollectDeviceIdentifiers",
        "*.R.bool.com.crashlytics.CollectUserIdentifiers",
        "*.R.string.com.crashlytics.CollectUserIdentifiers",
        "*.R.string.com.crashlytics.ApiEndpoint",
        "*.R.string.com.crashlytics.android.build_id",
        "*.R.bool.com.crashlytics.RequireBuildId",
        "*.R.string.com.crashlytics.RequireBuildId",
        "*.R.bool.com.crashlytics.CollectCustomLogs",
        "*.R.string.com.crashlytics.CollectCustomLogs",
        "*.R.bool.com.crashlytics.Trace",
        "*.R.string.com.crashlytics.Trace",
        "*.R.string.com.crashlytics.CollectCustomKeys"
    )
    mergeDuplicateResources = true // allow the merge of duplicate resources
    enableFileFiltering = true
    enableFilterStrings = true
    fileFilterList = setOf( // file filter rules
        "META-INF/*",
        // "*/armeabi-v7a/*",
        // "*/arm64-v8a/*",
        // "*/x86/*",
        // "*/x86_64/*"
    )
    unusedStringFile = "path/to/your/unused_strings.txt" // strings will be filtered in this file
    localeWhiteList = setOf("en", "in", "fr") // keep en,en-xx,in,in-xx,fr,fr-xx and remove others locale.
}
```

Groovy DSL configuration:

```groovy
resChiper {
    enableObfuscation = true // by default res obfuscate is enabled
    obfuscationMode = "default" // ["dir", "file", "default"]
    obfuscatedBundleName = "reschiper-app.aab" // Obfuscated file name, must end with '.aab'
    // mappingFile = file("path/to/your/mapping.txt").toPath() // Mapping file used for incremental obfuscation
    whiteList = [
        // Whitelist rule (directory name to exclude)
        "res/raw", // raw dir will not be obfuscated
        "res/xml", // xml dir will not be obfuscated

        // Whitelist rule (file name to exclude)
        "res/raw/*", // all files inside raw directory will not be obfuscated
        "res/raw/success_tick.json", // success_tick.json file will not be obfuscated
        "res/xml/*", // all files inside xml directory will not be obfuscated

        // Whitelist rules (resource name to exclude)
        "*.R.raw.*",
        "*.R.xml.*",

        // for google-services
        "*.R.string.google_api_key",
        "*.R.string.google_app_id",
        "*.R.string.default_web_client_id",
        "*.R.string.gcm_defaultSenderId",
        "*.R.string.ga_trackingId",
        "*.R.string.firebase_database_url",
        "*.R.string.google_crash_reporting_api_key",
        "*.R.string.google_storage_bucket",
        "*.R.integer.google_play_services_version",

        // firebase
        "*.R.string.project_id",
        // firebase crashlytics
        "*.R.string.com.google.firebase.crashlytics.mapping_file_id",
        "*.R.bool.com.crashlytics.useFirebaseAppId",
        "*.R.string.com.crashlytics.useFirebaseAppId",
        "*.R.string.google_app_id",
        "*.R.bool.com.crashlytics.CollectDeviceIdentifiers",
        "*.R.string.com.crashlytics.CollectDeviceIdentifiers",
        "*.R.bool.com.crashlytics.CollectUserIdentifiers",
        "*.R.string.com.crashlytics.CollectUserIdentifiers",
        "*.R.string.com.crashlytics.ApiEndpoint",
        "*.R.string.com.crashlytics.android.build_id",
        "*.R.bool.com.crashlytics.RequireBuildId",
        "*.R.string.com.crashlytics.RequireBuildId",
        "*.R.bool.com.crashlytics.CollectCustomLogs",
        "*.R.string.com.crashlytics.CollectCustomLogs",
        "*.R.bool.com.crashlytics.Trace",
        "*.R.string.com.crashlytics.Trace",
        "*.R.string.com.crashlytics.CollectCustomKeys"
    ]
    mergeDuplicateResources = true // allow the merge of duplicate resources
    enableFileFiltering = true
    enableFilterStrings = true
    fileFilterList = [ // file filter rules
        "META-INF/*",
        // "*/armeabi-v7a/*",
        // "*/arm64-v8a/*",
        // "*/x86/*",
        // "*/x86_64/*"
    ]
    unusedStringFile = "path/to/your/unused_strings.txt" // strings will be filtered in this file
    localeWhiteList = [
        "en",
        "in",
        "fr"
    ] // keep en,en-xx,in,in-xx,fr,fr-xx and remove others locale.
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
