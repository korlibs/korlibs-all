package com.soywiz.klogger

object Console

expect fun Console.error(vararg msg: Any?): Unit
expect fun Console.log(vararg msg: Any?): Unit
inline fun Console.err_print(str: String) = print(str)
inline fun Console.out_print(str: String) = print(str)

expect annotation class ConsoleThreadLocal()
