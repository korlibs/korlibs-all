package com.dragonbones.util

import com.dragonbones.util.StrReader.*
import java.util.Arrays
import java.util.Objects

class StrReader @JvmOverloads constructor(private val str: String, private var offset: Int = 0) {

    fun hasMore(): Boolean {
        return offset() < length()
    }

    fun eof(): Boolean {
        return offset() >= length()
    }

    fun peek(): Char {
        return str[offset]
    }

    fun read(): Char {
        return str[offset++]
    }

    fun offset(): Int {
        return offset
    }

    fun length(): Int {
        return str.length
    }

    fun available(): Int {
        return length() - offset()
    }

    fun peek(count: Int): String {
        return str.substring(offset, offset + Math.min(count, available()))
    }

    fun read(count: Int): String {
        val out = peek(count)
        skip(out.length)
        return out
    }

    @JvmOverloads
    fun skip(count: Int = 1) {
        offset += Math.min(count, available())
    }

    fun tryRead(c: Char): Boolean {
        if (peek() == c) {
            skip()
            return true
        } else {
            return false
        }
    }

    fun tryRead(value: String): String? {
        val read = peek(value.length)
        if (read == value) {
            skip(read.length)
            return read
        } else {
            return null
        }
    }

    fun tryRead(vararg values: String): String? {
        for (value in values) {
            val out = tryRead(value)
            if (out != null) return out
        }
        return null
    }

    fun expect(expect: Char): Char {
        val value = peek()
        if (value != expect) throw ParseException("Expected $expect")
        skip()
        return value
    }

    fun expect(expect: String): String {
        return tryRead(expect) ?: throw ParseException("Expected $expect")
    }

    fun expect(vararg expect: String): String {
        val offset = this.offset()
        return tryRead(*expect) ?: throw ParseException("Expected " + Arrays.asList(*expect) + " at " + offset)
    }

    fun skipSpaces() {
        while (hasMore()) {
            val c = peek()
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                skip(1)
            } else {
                break
            }
        }
    }

    class ParseException : RuntimeException {
        constructor() : super() {}

        constructor(s: String) : super(s) {}
    }
}
