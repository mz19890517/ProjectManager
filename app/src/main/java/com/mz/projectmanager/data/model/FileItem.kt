package com.mz.projectmanager.data.model

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val permissions: String = ""
) {
    val extension: String
        get() = if (isDirectory) "" else name.substringAfterLast('.', "")

    val isHidden: Boolean
        get() = name.startsWith(".")
}
