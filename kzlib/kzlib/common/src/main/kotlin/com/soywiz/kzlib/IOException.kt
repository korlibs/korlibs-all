package com.soywiz.kzlib

open class IOException(msg: String) : Exception(msg)
open class EOFException(msg: String) : IOException(msg)