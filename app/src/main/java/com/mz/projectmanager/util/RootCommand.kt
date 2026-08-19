package com.mz.projectmanager.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object RootCommand {

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val error = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(Exception(error.trim().ifEmpty { "Command failed with exit code $exitCode" }))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createDirectory(path: String): Result<String> {
        return execute("mkdir -p \"$path\"")
    }

    suspend fun deletePath(path: String): Result<String> {
        return execute("rm -rf \"$path\"")
    }

    suspend fun renamePath(oldPath: String, newPath: String): Result<String> {
        return execute("mv \"$oldPath\" \"$newPath\"")
    }

    suspend fun copyPath(source: String, dest: String): Result<String> {
        return execute("cp -r \"$source\" \"$dest\"")
    }

    suspend fun listFiles(path: String): Result<String> {
        return execute("ls -la \"$path\"")
    }

    suspend fun getFileStat(path: String): Result<String> {
        return execute("stat \"$path\"")
    }

    suspend fun createFile(path: String): Result<String> {
        return execute("touch \"$path\"")
    }

    suspend fun pathExists(path: String): Boolean {
        return execute("test -e \"$path\"").isSuccess
    }

    suspend fun isDirectory(path: String): Boolean {
        return execute("test -d \"$path\"").isSuccess
    }
}
