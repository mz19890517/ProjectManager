package com.mz.projectmanager.util

import java.security.SecureRandom

object IdGenerator {
    private val random = SecureRandom()

    fun generateProjectId(): String {
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateSessionId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder("ses_")
        for (i in 0 until 20) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }
}
