package com.soywiz.dynarek.js

import com.soywiz.dynarek.*

class JsGenerator(val func: DFunction, val log: Boolean = false) {
	val locals = func.getLocals()
	val localsToName = locals.withIndex().map { it.value to "T${it.index}" }.toMap()
	val DLocal<*>.name: String get() = localsToName[this] ?: "_UNKNOWN_"

	val <T> DExpr<T>.str: String
		get() = when (this) {
			is DLiteral<*> -> "$value"
			is DArg<*> -> "p$index"
			is DBinopInt -> {
				when (op) {
					IBinop.MUL -> "Math.imul(${left.str}, ${right.str})"
					else -> "((${left.str} ${op.op} ${right.str})|0)"
				}
			}
			is DBinopFloat -> "Math.fround(${left.str} ${op.op} ${right.str})"
			is DBinopIntBool -> "((${left.str} ${op.op} ${right.str}))"
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

	fun DStm.genJs(w: StringBuilder): Unit = when (this) {
		is DReturnVoid -> run { w.append("return;"); Unit }
		is DReturnExpr<*> -> run { w.append("return ${expr.str};"); Unit }
		is DStmExpr -> run { w.append("${expr.str};"); Unit }
		is DAssign<*> -> {
			val l = left
			val r = value.str
			when (l) {
				is DFieldAccess<*, *> -> {
					val objs = l.obj.str
					val propName = l.prop.name
					w.append("$objs.$propName = $r;")
				}
				is DLocal<*> -> {
					w.append("${l.name} = $r;")
				}
				else -> TODO("Unhandled.DStm.DAssign.genJs: $this")
			}
			Unit
		}
		is DStms -> for (stm in stms) stm.genJs(w)
		is DIfElse -> {
			w.append("if (${cond.str}) {")
			strue.genJs(w)
			w.append("}")
			if (sfalse != null) {
				w.append("else {")
				sfalse?.genJs(w)
				w.append("}")
			}
			Unit
		}
		is DWhile -> {
			w.append("while (${cond.str}) {")
			block.genJs(w)
			w.append("}")
			Unit
		}
		else -> TODO("Unhandled.DStm.genJs: $this")
	}

	fun generate(strict: Boolean): String {
		val sb = StringBuilder()
		if (strict
			) sb.append("\"use strict\";")
		for (local in locals) {
			sb.append("var ${local.name} = ${local.initialValue.str};")
		}
		func.body.genJs(sb)
		return sb.toString()
	}
}

fun DFunction.generateJsBody(strict: Boolean = true) = JsGenerator(this).generate(strict)
