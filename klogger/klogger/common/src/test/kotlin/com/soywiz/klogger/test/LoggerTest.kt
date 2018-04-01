package com.soywiz.klogger.test

import com.soywiz.klogger.LogLevel
import com.soywiz.klogger.LogOutput
import com.soywiz.klogger.Logger
import org.junit.Test
import kotlin.test.assertEquals

class LoggerTest {
    @Test
    fun simple() {
        val out = arrayListOf<String>()
        val logger = Logger("demo")
        logger.output = object : LogOutput {
            override fun output(logger: Logger, level: LogLevel, msg: String) {
                out += "${logger.name}: $level: $msg"
            }
        }
        logger.level = LogLevel.INFO
        logger.warn { "mywarn" }
        logger.info { "myinfo" }
        logger.trace { "mytrace" }
        assertEquals(listOf(
                "demo: WARN: mywarn",
                "demo: INFO: myinfo"
        ), out)
        logger.level = LogLevel.WARN
        logger.warn { "mywarn" }
        logger.info { "myinfo" }
        logger.trace { "mytrace" }
        assertEquals(listOf(
                "demo: WARN: mywarn",
                "demo: INFO: myinfo",
                "demo: WARN: mywarn"
        ), out)
    }
}