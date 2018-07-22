package com.dragonbones.util.json

import com.dragonbones.util.Array
import com.dragonbones.util.StrReader

import java.util.HashMap
import java.util.Objects

object JSON {
    fun parse(json: String): Any? {
        return parse(StrReader(json))
    }

    fun parse(s: StrReader): Any? {
        s.skipSpaces()
        when (s.peek()) {
            '{' -> return parseObject(s)
            '[' -> return parseArray(s)
            '"' -> return parseString(s)
            't', 'f' -> return parseBool(s)
            'n' -> return parseNull(s)
            '.', '+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'e', 'E' -> return parseNumber(s)
        }
        throw StrReader.ParseException("Unexpected character " + s.peek())
    }

    fun parseObject(s: StrReader): Any {
        val out = HashMap<String, Any>()
        s.skipSpaces()
        s.expect('{')
        s.skipSpaces()
        if (s.peek() != '}') {
            while (s.hasMore()) {
                s.skipSpaces()
                val key = Objects.toString(parse(s))
                s.skipSpaces()
                s.expect(':')
                s.skipSpaces()
                val value = parse(s)
                out[key] = value
                s.skipSpaces()
                val c = s.peek()
                if (c == ',') {
                    s.skip()
                    continue
                }
                if (c == '}') break
                throw StrReader.ParseException()
            }
        }
        s.skipSpaces()
        s.expect('}')

        return out
    }

    fun parseArray(s: StrReader): Array<*> {
        val out = Array<Any>()
        s.skipSpaces()
        s.expect('[')
        s.skipSpaces()
        if (s.peek() != ']') {
            while (s.hasMore()) {
                s.skipSpaces()
                out.push(parse(s))
                s.skipSpaces()
                val c = s.peek()
                if (c == ',') {
                    s.skip()
                    continue
                }
                if (c == ']') break
                throw StrReader.ParseException()
            }
        }
        s.skipSpaces()
        s.expect(']')
        s.skipSpaces()
        return out
    }

    fun parseString(s: StrReader): String {
        val sb = StringBuilder()
        s.skipSpaces()
        s.expect('"')
        while (s.hasMore()) {
            val c = s.peek()
            if (c == '"') {
                break
            } else if (c == '\\') {
                s.skip()
                val cc = s.read()
                when (cc) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\f')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'u' -> sb.appendCodePoint(Integer.parseInt(s.read(4), 16))
                    else -> throw StrReader.ParseException("Invalid $cc")
                }
            } else {
                sb.append(c)
                s.skip()
            }
        }
        s.expect('"')
        s.skipSpaces()
        return sb.toString()
    }

    fun parseNumber(s: StrReader): Double {
        val sb = StringBuilder()
        s.skipSpaces()
        loop@ while (s.hasMore()) {
            val c = s.peek()
            when (c) {
                '.', '+', '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'e', 'E' -> {
                    sb.append(c)
                    s.skip()
                }
                else -> break@loop
            }
        }
        s.skipSpaces()
        return java.lang.Double.parseDouble(sb.toString())
    }

    fun parseBool(s: StrReader): Boolean {
        s.skipSpaces()
        val v = s.expect("true", "false")
        s.skipSpaces()
        return v == "true"
    }

    fun parseNull(s: StrReader): Any? {
        s.skipSpaces()
        s.expect("null")
        s.skipSpaces()
        return null
    }
}
