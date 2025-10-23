package com.morizero.milthmlang.collector.action

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.morizero.milthmlang.collector.exception.WeblateException
import com.morizero.milthmlang.collector.model.VirtualFile
import com.morizero.milthmlang.collector.weblate.WeblateClient
import okhttp3.OkHttpClient
import org.gradle.api.file.DirectoryProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.slf4j.LoggerFactory

interface WeblateFetchTranslationParameter : WorkParameters {
    var endpoint: String
    var weblateToken: String
    var project: String
    var components: List<String>
    var ignoreKeys: List<String>
    var lang: String
    var outputDir: DirectoryProperty
}

abstract class WeblateFetchTranslationAction : WorkAction<WeblateFetchTranslationParameter> {
    override fun execute() {
        val logger = LoggerFactory.getLogger(this::class.java)

        val param = parameters

        val endpoint = param.endpoint
        val weblateToken = param.weblateToken
        val components = param.components
        val project = param.project
        val ignoreKeys = param.ignoreKeys
        val lang = param.lang
        val outputDirectory = param.outputDir

        val client = OkHttpClient()
        val mapper = jacksonObjectMapper()

        val weblateClient = WeblateClient(client) { conf ->
            conf.endpoint = endpoint
            conf.token = weblateToken
        }

        val translations = mutableMapOf<String, String>()
        // TODO: Update Only Mode

        for (component in components) {
            try {
                logger.info("Fetching component '$component' for language '${lang}'...")
                val tmp = weblateClient.weblateGet<Map<String, String>>(
                    "/translations/$project/$component/${lang}/file/?format=json"
                )
                for ((key, value) in tmp) {
                    if (key in ignoreKeys) {
                        continue
                    }
                    if (key in translations) {
                        logger.error("Duplicate key '$key' found in component '$component' for language '${lang}'. Overwriting previous value.")
                    }
                    translations[key] = value
                }
            } catch (e: WeblateException) {
                if (e.statusCode == 404) {
                    logger.warn("Component '$component' for language '${lang}' not found (404). Skipping.")
                    continue
                } else {
                    error("Failed to fetch component '$component' for language '${lang}': ${e.message}, \n${e}")
                }
            } catch (e: Exception) {
                error("Failed to fetch component '$component' for language '${lang}': ${e.message}, \n${e}")
            }
        }

        VirtualFile(
            path = "${lang}.json",
            content = mapper.writeValueAsBytes(translations)
        ).writeTo(outputDirectory)
    }
}
