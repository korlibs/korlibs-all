package com.soywiz.dynarek

// @TODO: A fast interpreter would require converting to a fast IR first
@Suppress("UNCHECKED_CAST")
class DSlowInterpreter(val args: List<*>, var retval: Any? = null) {
	private val _localsSet = LinkedHashSet<DLocal<*>>()
	private val _locals = LinkedHashMap<DLocal<*>, Any?>() // Super slow

	private fun <T : Any> DLocal<T>.initOnce(): DLocal<T> = this.apply {
		if (this !in _localsSet) {
			_localsSet += this
			_locals[this] = interpret(this.initialValue)
			//println("INITIAL_SET[$this] = ${interpret(initialValue)}")
		}
	}

	var <T : Any> DLocal<T>.value: T
		set(value: T) {
			//println("SET[$this] = $value")
			_locals[initOnce()] = value
		}
		get() {
			//println("GET[$this] = ${_locals[this]}")
			return _locals[initOnce()] as T
		}

	fun <T> interpret(node: DExpr<T>): T = when (node) {
		is DLiteral<*> -> node.value as T
		is DArg<*> -> args[node.index] as T
		is DBinopInt -> {
			//println("NODE: $node")
			val l = interpret(node.left)
			val r = interpret(node.right)
			//println("---")
			when (node.op) {
				IBinop.ADD -> (l + r)
				IBinop.SUB -> (l - r)
				IBinop.MUL -> (l * r)
				IBinop.DIV -> (l / r)
				IBinop.REM -> (l % r)
				IBinop.AND -> (l and r)
				IBinop.OR -> (l or r)
				IBinop.XOR -> (l xor r)
				IBinop.SHL -> (l shl r)
				IBinop.SHR -> (l shr r)
				IBinop.USHR -> (l ushr r)
			} as T
		}
		is DBinopFloat -> {
			val l = interpret(node.left)
			val r = interpret(node.right)
			when (node.op) {
				FBinop.ADD -> (l + r)
				FBinop.SUB -> (l - r)
				FBinop.MUL -> (l * r)
				FBinop.DIV -> (l / r)
				FBinop.REM -> (l % r)
			} as T
		}
		is DBinopIntBool -> {
			val l = interpret(node.left)
			val r = interpret(node.right)
			when (node.op) {
				Compop.EQ -> (l == r)
				Compop.NE -> (l != r)
				Compop.LE -> (l <= r)
				Compop.LT -> (l < r)
				Compop.GE -> (l >= r)
				Compop.GT -> (l > r)
			} as T
		}
		is DBinopFloatBool -> {
			val l = interpret(node.left)
			val r = interpret(node.right)
			when (node.op) {
				Compop.EQ -> (l == r)
				Compop.NE -> (l != r)
				Compop.LE -> (l <= r)
				Compop.LT -> (l < r)
				Compop.GE -> (l >= r)
				Compop.GT -> (l > r)
			} as T
		}
		is DFieldAccess<*, *> -> {
			val obj = interpret(node.obj)
			(node as DFieldAccess<Any, Any?>).prop.get(obj) as T
		}
		is DLocal<*> -> node.value as T
		is DExprInvoke<*, *> -> {
			when (node) {
				is DExprInvoke1<*, *> -> (node as DExprInvoke1<Any, Any>).func(
					interpret(node.p0)
				) as T
				is DExprInvoke2<*, *, *> -> (node as DExprInvoke2<Any, Any, Any>).func(
					interpret(node.p0), interpret(node.p1)
				) as T
				is DExprInvoke3<*, *, *, *> -> (node as DExprInvoke3<Any, Any, Any, Any>).func(
					interpret(node.p0), interpret(node.p1), interpret(node.p2)
				) as T
				is DExprInvoke4<*, *, *, *, *> -> (node as DExprInvoke4<Any, Any, Any, Any, Any>).func(
					interpret(node.p0), interpret(node.p1), interpret(node.p2), interpret(node.p3)
				) as T
				is DExprInvoke5<*, *, *, *, *, *> -> (node as DExprInvoke5<Any, Any, Any, Any, Any, Any>).func(
					interpret(node.p0), interpret(node.p1), interpret(node.p2), interpret(node.p3), interpret(node.p4)
				) as T
				else -> TODO("interpret.DExprInvoke: Not implemented $node")
			}
		}
		else -> TODO("interpret: Not implemented $node")
	}

	fun interpret(node: DStm): Boolean {
		when (node) {
			is DStms -> {
				for (stm in node.stms) if (interpret(stm)) return true
			}
			is DStmExpr -> {
				interpret(node.expr)
			}
			is DAssign<*> -> {
				val left = node.left
				val value = interpret(node.value)
				when (left) {
					is DFieldAccess<*, *> -> {
						val obj = interpret(left.obj)
						(left as DFieldAccess<Any, Any?>).prop.set(obj, value)
					}
					is DLocal<*> -> {
						left.value = value as Any
						//println("locals: $_locals")
					}
					else -> TODO("DAssign: Not implemented left assign ${node.left}")
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
			}
			is DWhile -> {
				while (interpret(node.cond)) {
					if (interpret(node.block)) return true
				}
			}
			is DReturnVoid -> {
				return true
			}
			is DReturnExpr<*> -> {
				retval = interpret(node.expr)
				return true
			}
			else -> TODO("DStm: Not implemented $node")
		}
		return false
	}

	fun interpret(func: DFunction): Any? {
		interpret(func.body)
		return retval
	}
}

