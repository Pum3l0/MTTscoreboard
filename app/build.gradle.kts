import java.net.URI
import java.util.Properties
import org.gradle.work.DisableCachingByDefault

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localSigning = rootProject.file("signing.properties")

android {
    namespace = "io.github.pum3l0.scoreboard"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.pum3l0.scoreboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    if (localSigning.isFile) {
        val keys = Properties().apply { localSigning.inputStream().use { load(it) } }
        val releaseKeys = signingConfigs.create("release") {
            storeFile = rootProject.file(requireNotNull(keys.getProperty("storeFile")))
            storePassword = requireNotNull(keys.getProperty("storePassword"))
            keyAlias = requireNotNull(keys.getProperty("keyAlias"))
            keyPassword = requireNotNull(keys.getProperty("keyPassword"))
        }
        buildTypes.getByName("release").signingConfig = releaseKeys
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.material3:material3:1.5.0")
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    testImplementation("junit:junit:4.13.2")
}

ksp {
    arg("room.schemaLocation", layout.projectDirectory.dir("schemas").asFile.absolutePath)
}

@DisableCachingByDefault(because = "The bundled font is reused from the generated assets directory")
abstract class BundleNunito : DefaultTask() {
    @get:Input
    abstract val offline: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    init {
        outputs.upToDateWhen { outputDirectory.file("fonts/nunito.ttf").get().asFile.isFile }
    }

    @TaskAction
    fun bundle() {
        val target = outputDirectory.file("fonts/nunito.ttf").get().asFile
        if (target.isFile || offline.get()) return
        val url = "https://raw.githubusercontent.com/googlefonts/nunito/" +
            "8c6a9bb9732545b9ed53f29ec5e1ab0ff53c4e6f/fonts/variable/Nunito%5Bwght%5D.ttf"
        try {
            val connection = URI.create(url).toURL().openConnection().apply {
                connectTimeout = 4000
                readTimeout = 4000
            }
            val limit = 2 * 1024 * 1024
            val font = connection.getInputStream().use { it.readNBytes(limit + 1) }
            check(font.size in 1024..limit && font.copyOfRange(0, 4).contentEquals(byteArrayOf(0, 1, 0, 0))) {
                "The download was not a TrueType font"
            }
            target.parentFile.mkdirs()
            target.writeBytes(font)
        } catch (exception: Exception) {
            logger.warn("Nunito could not be bundled. ScoreBoard will use Adwaita Sans. ${exception.message}")
        }
    }
}

androidComponents.onVariants { variant ->
    val fontTask = tasks.register<BundleNunito>("bundle${variant.name.replaceFirstChar { it.uppercaseChar() }}Nunito") {
        offline.set(gradle.startParameter.isOffline)
    }
    variant.sources.assets?.addGeneratedSourceDirectory(fontTask, BundleNunito::outputDirectory)
}
