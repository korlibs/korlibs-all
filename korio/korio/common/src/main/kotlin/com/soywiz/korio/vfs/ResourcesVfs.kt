package com.soywiz.korio.vfs

import com.soywiz.korio.*

val ResourcesVfs: VfsFile get() = KorioNative.ResourcesVfs
val ApplicationDataVfs: VfsFile get() = KorioNative.applicationDataVfs()
