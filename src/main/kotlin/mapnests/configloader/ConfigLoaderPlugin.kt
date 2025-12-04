package mapnests.configloader

import com.android.build.gradle.BaseExtension
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

class ConfigLoaderPlugin : Plugin<Project> {

    companion object {
        const val COM_ANDROID_LIBRARY = "com.android.library"
        const val COM_ANDROID_APPLICATION = "com.android.application"
        const val CLIENT_CONFIG_JSON_FILE_NAME = "bind-client-config.json"

        const val KEY_ID = "key_id"
        const val PACKAGES = "packages" // Updated from package_name
        const val PUBLIC_KEY = "public_key"
        const val ALG = "alg"
        const val HASH = "hash"
        const val SHA256 = "sha256"
        const val CLIENT_IDENTITY = "client_identity"
        const val KEY_IDENTIFIER = "key_identifier"
    }

    private val gson = Gson()

    data class ClientConfig(
        val keyId: String,
        val packages: List<String>, // Changed from packageName: String
        val publicKey: String,
        val alg: String,
        val HASH: String,
        val sha256s: List<String>, // Changed from SHA256: String
        val CLIENT_IDENTITY: String,
        val KEY_IDENTIFIER: String,
    )

    override fun apply(project: Project) {
        project.plugins.withId(COM_ANDROID_LIBRARY) {
            configureModule(project, isLibrary = true)
        }

        project.plugins.withId(COM_ANDROID_APPLICATION) {
            configureModule(project, isLibrary = false)
        }
    }

    private fun configureModule(project: Project, isLibrary: Boolean) {
        project.afterEvaluate {
            val config = loadClientConfig(project) ?: return@afterEvaluate

            val outputDir = project.layout.buildDirectory
                .dir(
                    if (isLibrary)
                        "generated/mapnests/config/src/main/java/com/mapnests/network/generated"
                    else
                        "generated/mapnests/config/src/main/kotlin/com/mapnests/mapsdk/generated"
                )
                .get().asFile
            outputDir.mkdirs()

            val outputFile = File(
                outputDir,
                if (isLibrary) "BindClientConfigLib.java" else "BindClientConfig.kt"
            )
            if (isLibrary) {
                outputFile.writeText(generateJavaConfig(config))
            } else {
                outputFile.writeText(generateKotlinConfig(config))
            }

            project.logger.lifecycle("📦✅ Generated ${outputFile.name} at ${outputFile.path}")

            // Add generated sources to source set
            val androidExtension = project.extensions.findByType(BaseExtension::class.java)
            androidExtension?.sourceSets?.getByName("main")?.java?.srcDir(outputDir)
        }
    }

    private fun loadClientConfig(project: Project): ClientConfig? {
        val jsonFile = File(project.rootProject.projectDir, CLIENT_CONFIG_JSON_FILE_NAME)
        if (!jsonFile.exists()) {
            project.logger.warn("⚠️ No $CLIENT_CONFIG_JSON_FILE_NAME found in ${project.rootProject.name}")
            return null
        }

        val jsonObj = gson.fromJson(jsonFile.readText(), JsonObject::class.java)
        return ClientConfig(
            keyId = jsonObj[KEY_ID]?.asString.orEmpty(),
            packages = jsonObj[PACKAGES]?.asJsonArray?.map { it.asString } ?: emptyList(),
            publicKey = jsonObj[PUBLIC_KEY]?.asString.orEmpty(),
            alg = jsonObj[ALG]?.asString.orEmpty(),
            HASH = jsonObj[HASH]?.asString.orEmpty(),
            sha256s = jsonObj[SHA256]?.asJsonArray?.map { it.asString } ?: emptyList(),
            CLIENT_IDENTITY = jsonObj[CLIENT_IDENTITY]?.asString.orEmpty(),
            KEY_IDENTIFIER = jsonObj[KEY_IDENTIFIER]?.asString.orEmpty(),
        )
    }

    private fun generateKotlinConfig(config: ClientConfig): String = """
        package com.mapnests.mapsdk.generated

        object BindClientConfig {
            const val KEY_ID = "${config.keyId}"
            @JvmField val PACKAGES = arrayOf(${config.packages.joinToString { "\"$it\"" }})
            const val PUBLIC_KEY = "${config.publicKey}"
            const val ALG = "${config.alg}"
            const val HASH = "${config.HASH}"
            @JvmField val SHA256 = arrayOf(${config.sha256s.joinToString { "\"$it\"" }})
            const val CLIENT_IDENTITY = "${config.CLIENT_IDENTITY}"
            const val KEY_IDENTIFIER = "${config.KEY_IDENTIFIER}"

        }
    """.trimIndent()

    private fun generateJavaConfig(config: ClientConfig): String = """
        package com.mapnests.network.generated;

        public final class BindClientConfigLib {
            private BindClientConfigLib() {}

            public static final String KEY_ID = "${config.keyId}";
            public static final String[] PACKAGES = new String[]{${config.packages.joinToString { "\"$it\"" }}};
            public static final String PUBLIC_KEY = "${config.publicKey}";
            public static final String ALG = "${config.alg}";
            public static final String HASH = "${config.HASH}";
            public static final String[] SHA256 = new String[]{${config.sha256s.joinToString { "\"$it\"" }}};
            public static final String CLIENT_IDENTITY = "${config.CLIENT_IDENTITY}";
            public static final String KEY_IDENTIFIER = "${config.KEY_IDENTIFIER}";
        }
    """.trimIndent()
}