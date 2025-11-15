package com.morizero.milthmlang.collector.action

import org.gradle.api.file.DirectoryProperty
import org.gradle.workers.WorkParameters

interface WeblateFetchTranslationParameter : WorkParameters {
    var endpoint: String
    var weblateToken: String
    var project: String
    var components: List<String>
    var acceptKey: WeblateAcceptKeyPredicator
    var lang: String
    var outputDir: DirectoryProperty
}
