plugins {
    kotlin("jvm") version "2.1.10"
    `java-gradle-plugin`
    `java-library`
}

group = "com.morizero.milthmlang"
version = "0.0.1"

repositories {
    mavenCentral()
}

object depVer {
    val jackson = "2.15.2"
}

dependencies {
    api("org.yaml", "snakeyaml", "+")

    api("com.fasterxml.jackson.core", "jackson-databind", depVer.jackson)
    api("com.fasterxml.jackson.module", "jackson-module-kotlin", depVer.jackson)
    api("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", depVer.jackson)
    api("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310", depVer.jackson)

    api(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    api("com.squareup.okhttp3:okhttp")
    api("com.squareup.okhttp3:logging-interceptor")

    api("org.apache.commons:commons-lang3:3.18.0")
}

gradlePlugin {
    plugins {
        create("milthm-translation-collector") {
            id = "com.morizero.milthmlangcollector"
            implementationClass = "com.morizero.milthmlang.collector.DataProcessorPlugin"
        }
    }
}
