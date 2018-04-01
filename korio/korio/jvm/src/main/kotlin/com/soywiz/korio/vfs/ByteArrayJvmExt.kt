package com.soywiz.korio.vfs

import java.io.*

suspend fun ByteArray.writeToFile(file: File) = LocalVfs(file).write(this)
