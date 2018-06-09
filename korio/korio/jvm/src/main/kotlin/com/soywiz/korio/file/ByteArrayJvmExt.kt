package com.soywiz.korio.file

import java.io.*

suspend fun ByteArray.writeToFile(file: File) = LocalVfs(file).write(this)
