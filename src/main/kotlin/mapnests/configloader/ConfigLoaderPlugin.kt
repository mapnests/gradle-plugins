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
        const val APP_ID = "app_id"
        const val PUBLIC_KEY = "public_key"
        const val ALG = "alg"
        const val HASH = "hash"
        const val DATA_IDENTITY = "data_identity"
        const val CLIENT_IDENTITY = "client_identity"
        const val KEY_IDENTIFIER = "key_identifier"
        const val PLATFORM = "platform"
    }

    private val gson = Gson()

    data class ClientConfig(
        val keyId: String,
        val appId: String,
        val publicKey: String,
        val alg: List<String>,
        val hash: String,
        val dataIdentity: String,
        val clientIdentity: String,
        val keyIdentifier: String,
        val platform: String
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
                .get()
                .asFile
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

            project.logger.lifecycle("📦 Generated ${outputFile.name} → ${outputFile.absolutePath}")

            // Register generated source
            project.extensions.findByType(BaseExtension::class.java)
                ?.sourceSets
                ?.getByName("main")
                ?.java
                ?.srcDir(outputDir)
        }
    }

    private fun loadClientConfig(project: Project): ClientConfig? {
        val jsonFile = File(project.rootProject.projectDir, CLIENT_CONFIG_JSON_FILE_NAME)

        if (!jsonFile.exists()) {
            project.logger.warn("⚠️ $CLIENT_CONFIG_JSON_FILE_NAME not found in root project")
            return null
        }

        val json = gson.fromJson(jsonFile.readText(), JsonObject::class.java)

        return ClientConfig(
            keyId = json[KEY_ID]?.asString.orEmpty(),
            appId = json[APP_ID]?.asString.orEmpty(),
            publicKey = json[PUBLIC_KEY]?.asString.orEmpty(),
            alg = json[ALG]?.asJsonArray?.map { it.asString } ?: emptyList(),
            hash = json[HASH]?.asString.orEmpty(),
            dataIdentity = json[DATA_IDENTITY]?.asString.orEmpty(),
            clientIdentity = json[CLIENT_IDENTITY]?.asString.orEmpty(),
            keyIdentifier = json[KEY_IDENTIFIER]?.asString.orEmpty(),
            platform = json[PLATFORM]?.asString.orEmpty()
        )
    }

    private fun generateKotlinConfig(config: ClientConfig): String = """
        package com.mapnests.mapsdk.generated

        object BindClientConfig {
            const val KEY_ID = "${config.keyId}"
            const val APP_ID = "${config.appId}"
            const val PUBLIC_KEY = "${config.publicKey}"
            @JvmField val ALG = arrayOf(${config.alg.joinToString { "\"$it\"" }})
            const val HASH = "${config.hash}"
            const val DATA_IDENTITY = "${config.dataIdentity}"
            const val CLIENT_IDENTITY = "${config.clientIdentity}"
            const val KEY_IDENTIFIER = "${config.keyIdentifier}"
            const val PLATFORM = "${config.platform}"
        }
    """.trimIndent()

    private fun generateJavaConfig(config: ClientConfig): String = """
        package com.mapnests.network.generated;

        public final class BindClientConfigLib {
            private BindClientConfigLib() {}

            public static final String KEY_ID = "${config.keyId}";
            public static final String APP_ID = "${config.appId}";
            public static final String PUBLIC_KEY = "${config.publicKey}";
            public static final String[] ALG = new String[]{${config.alg.joinToString { "\"$it\"" }}};
            public static final String HASH = "${config.hash}";
            public static final String DATA_IDENTITY = "${config.dataIdentity}";
            public static final String CLIENT_IDENTITY = "${config.clientIdentity}";
            public static final String KEY_IDENTIFIER = "${config.keyIdentifier}";
            public static final String PLATFORM = "${config.platform}";
        }
    """.trimIndent()
}
