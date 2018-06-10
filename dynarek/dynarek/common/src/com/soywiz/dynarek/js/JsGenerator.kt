package com.soywiz.dynarek.js

import com.soywiz.dynarek.*

class JsGenerator(val func: DFunction, val log: Boolean = false) {
	val locals = func.getLocals()
	val localsToName = locals.withIndex().map { it.value to "T${it.index}" }.toMap()
	val DLocal<*>.name: String get() = localsToName[this] ?: "_UNKNOWN_"

	val DIGITS = "0123456789ABCDEF"
	val Int.hex: String get() {
		var out = "0x"
		for (n in 7 downTo 0) out += DIGITS[(this ushr (n * 4)) and 0xF]
		return out
	}

	val <T> DExpr<T>.str: String
		get() = when (this) {
			is DLiteral<*> -> {
				if (kind == "hex") {
					"((" + (value as Int).hex + ")|0)"
				} else {
					"$value"
				}
			}
			is DArg<*> -> "p$index"
			is DBinopInt -> {
				when (op) {
					IBinop.MUL -> "Math.imul(${left.str}, ${right.str})"
					else -> "((${left.str} ${op.op} ${right.str})|0)"
				}
			}
			is DBinopFloat -> "Math.fround(${left.str} ${op.op} ${right.str})"
			is DBinopIntBool -> "((${left.str} ${op.op} ${right.str}))"
			is DBinopFloatBool -> "((${left.str} ${op.op} ${right.str}))"
			is DFieldAccess<*, *> -> "${obj.str}.${prop.name}"
			is DExprInvoke<*, *> -> {
				val obj = this.args.first()
				val args = this.args.drop(1)
				val argsSr = args.joinToString(", ") { it.str }
				"${obj.str}.${this.name}($argsSr)"
			}
			is DLocal<*> -> this.name
			else -> TODO("Unhandled.DExpr.genJs: $this")
		}

	fun DStm.genJs(w: StringBuilder, indent: Int): Unit = when (this) {
		is DReturnVoid -> run { w.indent(indent).append("return;\n"); Unit }
		is DReturnExpr<*> -> run { w.indent(indent).append("return ${expr.str};\n"); Unit }
		is DStmExpr -> run { w.indent(indent).append("${expr.str};\n"); Unit }
		is DAssign<*> -> {
			val l = left
			val r = value.str
			when (l) {
				is DFieldAccess<*, *> -> {
					val objs = l.obj.str
					val propName = l.prop.name
					w.indent(indent).append("$objs.$propName = $r;\n")
				}
				is DLocal<*> -> {
					w.indent(indent).append("${l.name} = $r;\n")
				}
				else -> TODO("Unhandled.DStm.DAssign.genJs: $this")
			}
			Unit
		}
		is DStms -> {
			for (stm in stms) stm.genJs(w, indent)
		}
		is DIfElse -> {
			w.indent(indent).append("if (${cond.str}) {\n")
			strue.genJs(w, indent + 1)
			if (sfalse != null) {
				w.indent(indent).append("} else {\n")
				sfalse?.genJs(w, indent + 1)
			}
			w.indent(indent).append("}\n")
			Unit
		}
		is DWhile -> {
			w.indent(indent).append("while (${cond.str}) {\n")
			block.genJs(w, indent + 1)
			w.indent(indent).append("}\n")
			Unit
		}
		else -> TODO("Unhandled.DStm.genJs: $this")
	}

	fun generate(strict: Boolean): String {
		val sb = StringBuilder()
		if (strict) sb.append("\"use strict\";")
		for (local in locals) {
			sb.append("var ${local.name} = ${local.initialValue.str};\n")
		}
		func.body.genJs(sb, 0)
		return sb.toString()
	}
}

fun StringBuilder.indent(count: Int): StringBuilder = append(INDENTS[count])

fun DFunction.generateJsBody(strict: Boolean = true) = JsGenerator(this).generate(strict)

object INDENTS {
	private val INDENTS = arrayListOf<String>("")

	operator fun get(index: Int): String {
		if (index >= INDENTS.size) {
			val calculate = INDENTS.size * 10
			var indent = INDENTS[INDENTS.size - 1]
			while (calculate >= INDENTS.size) {
				indent += "\t"
				INDENTS.add(indent)
			}
		}
		return if (index <= 0) "" else INDENTS[index]
	}
}
