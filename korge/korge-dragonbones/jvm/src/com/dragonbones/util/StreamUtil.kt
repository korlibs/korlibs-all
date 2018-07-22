package com.dragonbones.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

object StreamUtil {
    fun readAll(s: InputStream): ByteArray {
        val temp = ByteArray(1024)
        val os = ByteArrayOutputStream()
        try {
            while (true) {
                val read = s.read(temp, 0, temp.size)
                if (read <= 0) break
                os.write(temp, 0, read)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return os.toByteArray()
    }

    fun getResourceString(path: String, charset: Charset): String {
        try {
            return String(getResourceBytes(path), charset.name())
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }

    }

    fun getResourceBytes(path: String): ByteArray {
        val s = ClassLoader.getSystemClassLoader().getResourceAsStream(path)
        try {
            return readAll(s)
        } finally {
            try {
                s.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }
}
