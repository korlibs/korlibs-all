package com.soywiz.korio.file

import java.net.*

fun UrlVfs(url: URL): VfsFile = com.soywiz.korio.file.std.UrlVfs(url.toString())
