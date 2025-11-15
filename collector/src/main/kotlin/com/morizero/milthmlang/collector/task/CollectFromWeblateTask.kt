package com.morizero.milthmlang.collector.task

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.morizero.milthmlang.collector.action.WeblateAcceptKeyPredicator
import com.morizero.milthmlang.collector.action.WeblateFetchTranslationAction
import com.morizero.milthmlang.collector.model.ComponentMeta
import com.morizero.milthmlang.collector.model.LanguagePackMeta
import com.morizero.milthmlang.collector.model.VirtualFile
import com.morizero.milthmlang.collector.weblate.WeblateClient
import okhttp3.OkHttpClient
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkerExecutor
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject

abstract class CollectFromWeblateTask @Inject constructor(
    @Inject private val workerExecutor: WorkerExecutor
) : DefaultTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @Input
    lateinit var weblateToken: String

    @Input
    lateinit var acceptKeyPredicator: WeblateAcceptKeyPredicator

    @get:Internal
    abstract val latestWeblateEventId: Property<String>

    private val endpoint = "https://weblate.milthm.com/api"
    private val project = "milthm"
    private val components = listOf(
        "noun-and-term",
        "guidance-manual",
        "main",
        "settings",
        "miscellaneous",
        "story",
        "template",
        "avg",
        "web",
        "configuration-comment",
        "events",
        "garden",
        "error"
    )

    private val jsonMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)


    @TaskAction
    fun execute() {
        if (weblateToken.isBlank()) {
            throw IllegalArgumentException("Weblate token can not be empty")
        }

        val outputDirectory = outputDir.get().asFile
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        syncWeblateTranslation()
    }

    fun syncWeblateTranslation() {
        logger.info("Syncing Weblate translation from remote...")

        val client = OkHttpClient()

        val weblateClient = WeblateClient(client) { conf ->
            conf.endpoint = endpoint
            conf.token = weblateToken
        }

        val languages = weblateClient.getLanguages(project).filter { lang -> lang.code != "und" }
        logger.info("Languages: ${languages.joinToString(separator = ", ") { it.code }}")

        val meta = LanguagePackMeta()
        val masterMeta = ComponentMeta(
            lastId = 0,
            lastModifiedAt = java.time.LocalDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneOffset.UTC),
        )
        meta["__master"] = masterMeta
        for (component in components) {
            val changes = weblateClient.getComponentsChanges(project, component)
            val change = changes.results.first()
            meta[component] = ComponentMeta(
                lastId = change.id,
                lastModifiedAt = change.timestamp,
            )
            if (change.timestamp > masterMeta.lastModifiedAt) {
                masterMeta.lastModifiedAt = change.timestamp
                masterMeta.lastId = change.id
            }
        }
        logger.info("Component metas: ${jsonMapper.writeValueAsString(meta)}")
        latestWeblateEventId.set("${masterMeta.lastId}")
        VirtualFile(
            path = "__meta.json",
            content = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(meta)
        ).writeTo(outputDir)

        val workQueue = workerExecutor.noIsolation()
        for (lang in languages) {
            workQueue.submit(WeblateFetchTranslationAction::class.java) { param ->
                param.endpoint = endpoint
                param.weblateToken = weblateToken
                param.project = project
                param.components = components
                param.acceptKey = acceptKeyPredicator
                param.lang = lang.code
                param.outputDir = outputDir
            }
        }
        workQueue.await()
        logger.info("Successfully synced ${languages.size} languages.")
    }
}
