package com.mz.projectmanager.data.repository

import com.mz.projectmanager.data.model.FileItem
import com.mz.projectmanager.data.model.SortOption
import com.mz.projectmanager.util.RootCommand
import java.text.SimpleDateFormat
import java.util.Locale

class FileRepository(private val projectsRoot: String) {

    suspend fun listFiles(path: String, sortOption: SortOption = SortOption.NAME_ASC): Result<List<FileItem>> {
        val result = RootCommand.listFiles(path)
        return result.map { output ->
            parseLsOutput(output, path)
                .filter { !it.name.startsWith(".") || it.name == ".." }
                .let { applySorting(it, sortOption) }
        }
    }

    suspend fun getFileDetails(path: String): Result<FileItem> {
        val statResult = RootCommand.getFileStat(path)
        return statResult.map { stat ->
            parseStatOutput(stat, path)
        }
    }

    suspend fun createDirectory(parentPath: String, name: String): Result<String> {
        val fullPath = "$parentPath/$name"
        return RootCommand.createDirectory(fullPath)
    }

    suspend fun createFile(parentPath: String, name: String): Result<String> {
        val fullPath = "$parentPath/$name"
        return RootCommand.createFile(fullPath)
    }

    suspend fun delete(path: String): Result<String> {
        return RootCommand.deletePath(path)
    }

    suspend fun rename(oldPath: String, newName: String): Result<String> {
        val parent = oldPath.substringBeforeLast('/')
        val newPath = "$parent/$newName"
        return RootCommand.renamePath(oldPath, newPath)
    }

    suspend fun copy(source: String, destDir: String): Result<String> {
        return RootCommand.copyPath(source, destDir)
    }

    suspend fun move(source: String, destDir: String): Result<String> {
        return RootCommand.renamePath(source, "$destDir/${source.substringAfterLast('/')}")
    }

    private fun parseLsOutput(output: String, basePath: String): List<FileItem> {
        val lines = output.lines().filter { it.isNotBlank() }
        val items = mutableListOf<FileItem>()

        for (line in lines) {
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 8) continue

            val permissions = parts[0]
            val name = parts.drop(8).joinToString(" ")
            if (name.isEmpty() || name == "." || name == "..") continue

            val isDir = permissions.startsWith("d")
            val size = if (!isDir) {
                try { parts[4].toLongOrNull() ?: 0L } catch (e: Exception) { 0L }
            } else 0L

            val fullPath = "$basePath/$name"

            items.add(
                FileItem(
                    name = name,
                    path = fullPath,
                    isDirectory = isDir,
                    size = size,
                    permissions = permissions
                )
            )
        }
        return items
    }

    private fun parseStatOutput(stat: String, path: String): FileItem {
        val name = path.substringAfterLast('/')
        val isDir = stat.contains("File:") && stat.contains("directory") ||
                path.endsWith("/")
        val sizeMatch = "Size:\\s*(\\d+)".toRegex().find(stat)
        val size = sizeMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val permMatch = "Access:\\s*\\(([^)]+)\\)".toRegex().find(stat)
        val permissions = permMatch?.groupValues?.get(1) ?: ""

        return FileItem(
            name = name,
            path = path,
            isDirectory = isDir,
            size = size,
            permissions = permissions
        )
    }

    private fun applySorting(items: List<FileItem>, sortOption: SortOption): List<FileItem> {
        val dirs = items.filter { it.isDirectory }
        val files = items.filter { !it.isDirectory }

        val sortedDirs = when (sortOption) {
            SortOption.NAME_ASC -> dirs.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> dirs.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> dirs.sortedByDescending { it.lastModified }
            SortOption.DATE_OLDEST -> dirs.sortedBy { it.lastModified }
            SortOption.SIZE_LARGEST -> dirs.sortedByDescending { it.size }
            SortOption.SIZE_SMALLEST -> dirs.sortedBy { it.size }
        }

        val sortedFiles = when (sortOption) {
            SortOption.NAME_ASC -> files.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> files.sortedByDescending { it.name.lowercase() }
            SortOption.DATE_NEWEST -> files.sortedByDescending { it.lastModified }
            SortOption.DATE_OLDEST -> files.sortedBy { it.lastModified }
            SortOption.SIZE_LARGEST -> files.sortedByDescending { it.size }
            SortOption.SIZE_SMALLEST -> files.sortedBy { it.size }
        }

        return sortedDirs + sortedFiles
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
            else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
        }
    }

    fun formatTimestamp(millis: Long): String {
        if (millis == 0L) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(millis)
    }
}
