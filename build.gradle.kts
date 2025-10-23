import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mgd.core.gradle.S3Upload
import com.morizero.milthmlang.collector.model.LanguagePackMeta
import com.morizero.milthmlang.collector.task.CollectFromWeblateTask

plugins {
    id("com.morizero.milthmlangcollector")
    id("signing")
    id("com.mgd.core.gradle.s3") version "3.0.0"
}

group = "com.morizero.milthm-translation-collector"
version = "0.0.1"

subprojects {
    group = "com.morizero.milthmlang"
    version = "0.0.1"
    repositories { repo() }
}

repositories { repo() }

fun RepositoryHandler.repo() {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

s3 {
    endpoint = providers.gradleProperty("s3.endpoint").orNull ?: ""
    region = providers.gradleProperty("s3.region").orNull ?: ""
}

private val jsonMapper: ObjectMapper = ObjectMapper().registerModule(JavaTimeModule()).registerKotlinModule()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)


tasks {
    val weblateTask = named<CollectFromWeblateTask>("weblate") {
        val ignoredKeysEnv = providers.gradleProperty("weblate.ignoredKeys").orNull ?: ""
        val ignoredKeysList = ignoredKeysEnv.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        outputDir = project.layout.buildDirectory.asFile.get().resolve("weblate")
        weblateToken = providers.gradleProperty("weblate.token").orNull ?: ""
        ignoreKeys = ignoredKeysList
    }

    val signWeblate = register<Sign>("signWeblate") {
        group = "signing"
        description = "GPG-sign all files produced by the weblate task."
        dependsOn(weblateTask)

        val outDir = weblateTask.get().outputDir.asFile
        val filesProvider = outDir.map { dir ->
            project.fileTree(dir) {
                include("**/*")
                exclude("**/*.asc", "**/*.sig")
                exclude { it.file.isDirectory }
            }
        }

        inputs.files(filesProvider)

        doFirst {
            val files = filesProvider.get().files
            if (files.isEmpty()) {
                logger.error("No files to sign in ${outDir.get().absoluteFile}")
            } else {
                sign(*files.toTypedArray())
            }
        }

        onlyIf {
            providers.gradleProperty("signing.secretKeyRingFile").isPresent
        }
    }

    val artifactFile = weblateTask.flatMap {
        val metaPath = it.outputDir.asFile.get().resolve("__meta.json")
        var meta = jsonMapper.readValue<LanguagePackMeta>(metaPath.readText(Charsets.UTF_8))
        val masterMeta = meta["__master"] ?: throw IllegalStateException("Master meta not found")
        provider { "milthm-translations-${masterMeta.lastId}.zip" }
    }

    val artifactZipTask = register<Zip>("artifactZip") {
        group = "distribution"
        description = "Package weblate outputs and signatures into artifacts.zip"
        dependsOn(signWeblate)

        val weblateOutDir = weblateTask.flatMap { it.outputDir }
        from(weblateOutDir) {
            include("**/*")
        }

        destinationDirectory.set(project.layout.buildDirectory.dir("distributions"))
        archiveFileName.set(artifactFile)

        isPreserveFileTimestamps = true
        isReproducibleFileOrder = true
    }

    val uploadToS3 = register<S3Upload>("uploadToS3") {
        System.setProperty("aws.accessKeyId", providers.gradleProperty("s3.accessKeyId").orNull ?: "")
        System.setProperty("aws.secretAccessKey", providers.gradleProperty("s3.secretAccessKey").orNull ?: "")

        group = "upload"
        description = "Upload the artifact zip to S3"
        dependsOn(artifactZipTask)

        val s3Bucket = providers.gradleProperty("s3.bucket").orNull ?: ""
        val s3KeyPrefix = providers.gradleProperty("s3.keyPrefix").orNull ?: ""

        bucket = s3Bucket
        doFirst {
            val computedKey = artifactFile.map { fileName ->
                if (s3KeyPrefix.isNotEmpty()) {
                    "$s3KeyPrefix/$fileName"
                } else {
                    fileName
                }
            }
            key = computedKey.get()

            val destDir = artifactZipTask.flatMap { it.destinationDirectory }
            file = destDir.get().asFile.resolve(artifactFile.get()).absolutePath
        }

        overwrite = true

        onlyIf {
            s3.endpoint.isNotEmpty() && s3.region.isNotEmpty() && s3Bucket.isNotEmpty()
        }
    }
}
