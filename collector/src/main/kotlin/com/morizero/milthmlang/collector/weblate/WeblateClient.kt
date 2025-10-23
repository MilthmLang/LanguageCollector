package com.morizero.milthmlang.collector.weblate

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.morizero.milthmlang.collector.exception.WeblateException
import com.morizero.milthmlang.collector.model.ChangeResultRoot
import com.morizero.milthmlang.collector.model.ComponentsResultRoot
import com.morizero.milthmlang.collector.model.LanguageListItem
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WeblateClientConfigurer {
    var endpoint: String = "https://weblate.milthm.com/api"
    var token: String = ""
}

class WeblateClient(
    val client: OkHttpClient = OkHttpClient(),
    configurer: (WeblateClientConfigurer) -> Unit = {}
) {
    val jsonMapper: ObjectMapper =
        ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val config = WeblateClientConfigurer()

    init {
        configurer(config)
    }

    val endpoint: String = config.endpoint

    val token: String = config.token

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    public inline fun <reified T> weblateGet(url: String): T {
        val req = Request.Builder().url(endpoint + url).get().addHeader("Authorization", "Token $token")
            .addHeader("Accept", "application/json").build()

        return client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) {
                throw WeblateException(res.code, "failed to fetch data: ${res.code}, url: $url")
            }
//            val body = res.body?.string() ?: ""
//            logger.info(body)
//            jsonMapper.readValue(body, object : TypeReference<T>() {})
            jsonMapper.readValue(res.body?.byteStream(), object : TypeReference<T>() {})
        }
    }

    fun getLanguages(project: String): List<LanguageListItem> {
        return weblateGet<List<LanguageListItem>>("/projects/$project/languages/")
    }

    fun getComponents(project: String): ComponentsResultRoot {
        return weblateGet("/projects/$project/components/")
    }

    fun getComponentsChanges(project: String, components: String): ChangeResultRoot {
        return weblateGet("/components/$project/$components/changes/")
    }
}
