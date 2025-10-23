package com.morizero.milthmlang.collector.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

class LanguageListItem {
    @JsonProperty("code")
    var code: String = ""
}

class CompoentResultProject {
    @JsonProperty("name")
    var name: String = ""

    @JsonProperty("slug")
    var slug: String = ""

    @JsonProperty("id")
    val id: Long = 0

}

class ComponentsResult {
    @JsonProperty("slug")
    var slug: String = ""

    @JsonProperty("project")
    var project: CompoentResultProject = CompoentResultProject()

    @JsonProperty("is_glossary")
    var isGlossary: Boolean = false
}

class ComponentsResultRoot {
    @JsonProperty("results")
    var results: MutableList<ComponentsResult> = mutableListOf()
}

class ChangeResult {
    @JsonProperty("unit")
    var unit: String? = ""

    @JsonProperty("component")
    var component: String = ""

    @JsonProperty("translation")
    var translation: String? = ""

    @JsonProperty("user")
    var user: String? = ""

    @JsonProperty("author")
    var author: String? = ""

    @JsonProperty("timestamp")
    var timestamp: LocalDateTime = LocalDateTime.now()

    @JsonProperty("action")
    var action: Int = 0

    @JsonProperty("target")
    var target: String = ""

    @JsonProperty("old")
    var old: String = ""

    @JsonProperty("details")
    var details: Map<String, Any> = mapOf()

    @JsonProperty("id")
    var id: Long = 0

    @JsonProperty("action_name")
    var actionName: String = ""

    @JsonProperty("url")
    var url: String = ""
}

class ChangeResultRoot {
    @JsonProperty("results")
    var results: MutableList<ChangeResult> = mutableListOf()
}


