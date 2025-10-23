package com.morizero.milthmlang.collector.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

class LanguagePackMeta : MutableMap<String, ComponentMeta> by mutableMapOf()

data class ComponentMeta(
    @field:JsonProperty("last_id") var lastId: Long,
    @field:JsonProperty("last_modified_at") var lastModifiedAt: LocalDateTime
)