package com.soywiz.korte

sealed class Token {
	var trimLeft = false
	var trimRight = false

	data class TLiteral(val content: String) : Token()
	data class TExpr(val content: String) : Token()
	data class TTag(val name: String, val content: String) : Token() {
		val tokens by lazy { ExprNode.Token.tokenize(content) }
	}

	companion object {
		fun tokenize(str: String): List<Token> {
			val out = arrayListOf<Token>()
			var lastPos = 0

			fun emit(token: Token) {
				if (token is TLiteral && token.content.isEmpty()) return
				out += token
			}

			var pos = 0
			loop@ while (pos < str.length) {
				val c = str[pos++]
				// {# {% {{ }} %} #}
				if (c == '{') {
					if (pos >= str.length) break
					val c2 = str[pos++]
					when (c2) {
					// Comment
						'#' -> {
							val startPos = pos - 2
							if (lastPos != startPos) {
								emit(TLiteral(str.substring(lastPos until startPos)))
							}
							val endCommentP1 = str.indexOf("#}", startIndex = pos)
							val endComment = if (endCommentP1 >= 0) endCommentP1 + 2 else str.length
							lastPos = endComment
							pos = endComment
						}
						'{', '%' -> {
							val startPos = pos - 2
							val pos2 = if (c2 == '{') str.indexOf("}}", pos) else str.indexOf("%}", pos)
							if (pos2 < 0) break@loop
							val trimLeft = str[pos] == '-'
							val trimRight = str[pos2 - 1] == '-'

							val p1 = if (trimLeft) pos + 1 else pos
							val p2 = if (trimRight) pos2 - 1 else pos2

							val content = str.substring(p1, p2).trim()

							if (lastPos != startPos) {
								emit(TLiteral(str.substring(lastPos until startPos)))
							}

							val token = if (c2 == '{') {
								//println("expr: '$content'")
								TExpr(content)
							} else {
								val parts = content.split(' ', limit = 2)
								//println("tag: '$content'")
								TTag(parts[0], parts.getOrElse(1) { "" })
							}
							token.trimLeft = trimLeft
							token.trimRight = trimRight
							emit(token)
							pos = pos2 + 2
							lastPos = pos
						}
					}
				}
			}
			emit(TLiteral(str.substring(lastPos, str.length)))

			for ((n, cur) in out.withIndex()) {
				if (cur is Token.TLiteral) {
					val trimStart = out.getOrNull(n - 1)?.trimRight ?: false
					val trimEnd = out.getOrNull(n + 1)?.trimLeft ?: false
					out[n] = if (trimStart && trimEnd) {
						TLiteral(cur.content.trim())
					} else if (trimStart) {
						TLiteral(cur.content.trimStart())
					} else if (trimEnd) {
						TLiteral(cur.content.trimEnd())
					} else {
						cur
					}
				}
			}

			return out
		}
	}
}
