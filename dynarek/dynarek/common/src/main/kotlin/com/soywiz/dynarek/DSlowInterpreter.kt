package com.soywiz.dynarek

@Suppress("UNCHECKED_CAST")
class DSlowInterpreter(val args: List<*>, var retval: Any? = null) {
	fun <T> interpret(node: DExpr<T>): T = when (node) {
		is DLiteral<*> -> node.value as T
		is DArg<*> -> args[node.index] as T
		is DBinopInt -> {
			val l = interpret(node.left)
			val r = interpret(node.right)
			when (node.op) {
				IBinop.ADD -> (l + r) as T
				IBinop.MUL -> (l * r) as T
				else -> TODO("Unknown op ${node.op}")
			}
		}
		is DFieldAccess<*, *> -> {
			val obj = interpret(node.obj)
			(node as DFieldAccess<Any, Any?>).prop.get(obj) as T
		}
		else -> TODO("Not implemented $node")
	}

	fun interpret(node: DStm): Unit = when (node) {
		is DStms -> for (stm in node.stms) interpret(stm)
		is DAssign<*> -> {
			val left = node.left
			val value = interpret(node.value)
			when (left) {
				is DFieldAccess<*, *> -> {
					val obj = interpret(left.obj)
					(left as DFieldAccess<Any, Any?>).prop.set(obj, value)
				}
				else -> TODO("Not implemented left assign ${node.left}")
			}
		}
		is DIfElse -> {
			val cond = interpret(node.cond)
			val strue = node.strue
			val sfalse = node.sfalse
			if (cond) {
				interpret(strue)
			} else if (sfalse != null) {
				interpret(sfalse)
			}
			Unit
		}
		else -> TODO("Not implemented $node")
	}

	fun interpret(func: DFunction) {
		interpret(func.body)
	}
}

