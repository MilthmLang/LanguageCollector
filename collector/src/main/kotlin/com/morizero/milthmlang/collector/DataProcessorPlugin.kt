package com.morizero.milthmlang.collector

import com.morizero.milthmlang.collector.task.CollectFromWeblateTask
import org.gradle.api.Plugin
import org.gradle.api.Project

class DataProcessorPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.tasks.run {
            register("weblate", CollectFromWeblateTask::class.java)
        }
    }
}
