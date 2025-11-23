# Config Loader Gradle Plugin

This Gradle plugin, `com.mapnests.config-loader`, automates the process of loading client
configuration from a JSON file and generating a corresponding Kotlin or Java configuration file
within your Android project. This allows you to easily access configuration values at build time.

## How it works

The plugin performs the following steps:

1. **Reads Configuration:** It looks for a `bind-client-config.json` file in the root directory of
   your project.
2. **Generates Source Code:** Based on the content of the JSON file, it generates a
   `BindClientConfig.kt` (for Android applications) or `BindClientConfigLib.java` (for Android
   libraries) file.
3. **Adds to Source Set:** The generated file is automatically added to the `main` source set of
   your project, making the configuration accessible from your app or library code.

## Usecase

This plugin is useful when you need to embed configuration data into your Android application or
library that is determined at build time. For example, you can use it to manage different
environments (e.g., development, staging, production) by simply changing the
`bind-client-config.json` file.

## Setup

1. **Create `bind-client-config.json`:**
   In the root directory of your Android project, create a file named `bind-client-config.json` with
   the following structure:

   ```json
   {
     "key_id": "your_key_id",
     "package_name": "your_package_name",
     "public_key": "your_public_key",
     "alg": "your_algorithm"
   }
   ```

2. **Apply the plugin:**
   In your module's `build.gradle.kts` or `build.gradle` file, apply the plugin:

   ```kotlin
   plugins {
       id("com.mapnests.config-loader")
   }
   ```

## Usage

After a successful Gradle sync and build, you can access the configuration values directly in your
code:

**For Kotlin (Android Applications):**

```kotlin
import com.mapnests.mapsdk.generated.BindClientConfig

// ...

val keyId = BindClientConfig.KEY_ID
val packageName = BindClientConfig.PACKAGE_NAME
val publicKey = BindClientConfig.PUBLIC_KEY
val alg = BindClientConfig.ALG
```

**For Java (Android Libraries):**

```java
import com.mapnests.network.generated.BindClientConfigLib;

// ...

String keyId = BindClientConfigLib.KEY_ID;
String packageName = BindClientConfigLib.PACKAGE_NAME;
String publicKey = BindClientConfigLib.PUBLIC_KEY;
String alg = BindClientConfigLib.ALG;
```
