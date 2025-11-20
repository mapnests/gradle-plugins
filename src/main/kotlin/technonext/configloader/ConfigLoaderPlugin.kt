package technonext.configloader

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
        const val PACKAGE_NAME = "package_name"
        const val PUBLIC_KEY = "public_key"
        const val ALG = "alg"
    }

    private val gson = Gson()

    data class ClientConfig(
        val keyId: String,
        val packageName: String,
        val publicKey: String,
        val alg: String
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
                        "generated/technonext/config/src/main/java/com/technonext/network/generated"
                    else
                        "generated/technonext/config/src/main/kotlin/com/technonext/mapsdk/generated"
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
            packageName = jsonObj[PACKAGE_NAME]?.asString.orEmpty(),
            publicKey = jsonObj[PUBLIC_KEY]?.asString.orEmpty(),
            alg = jsonObj[ALG]?.asString.orEmpty()
        )
    }

    private fun generateKotlinConfig(config: ClientConfig): String = """
        package com.technonext.mapsdk.generated

        object BindClientConfig {
            const val KEY_ID = "${config.keyId}"
            const val PACKAGE_NAME = "${config.packageName}"
            const val PUBLIC_KEY = "${config.publicKey}"
            const val ALG = "${config.alg}"
        }
    """.trimIndent()

    private fun generateJavaConfig(config: ClientConfig): String = """
        package com.technonext.network.generated;

        public final class BindClientConfigLib {
            private BindClientConfigLib() {}

            public static final String KEY_ID = "${config.keyId}";
            public static final String PACKAGE_NAME = "${config.packageName}";
            public static final String PUBLIC_KEY = "${config.publicKey}";
            public static final String ALG = "${config.alg}";
        }
    """.trimIndent()
}
