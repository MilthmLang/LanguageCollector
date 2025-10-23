package com.morizero.milthmlang.collector.model

data class VirtualFile(
    val path: String,
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as VirtualFile

        if (path != other.path) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + content.contentHashCode()
        return result
    }

    fun writeTo(target: org.gradle.api.file.DirectoryProperty) {
        target.get().asFile.resolve(path).run {
            writeBytes(content)
        }
    }
}
