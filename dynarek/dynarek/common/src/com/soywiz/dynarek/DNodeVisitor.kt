package com.soywiz.dynarek

import kotlin.reflect.*

open class DNodeVisitor {
	open fun visit(func: DFunction): Unit {
		visit(func.ret)
		for (arg in func.args) visit(arg)
		visit(func.body)
	}

	open fun visit(node: DNode): Unit = when (node) {
		is DStm -> visit(node)
		is DExpr<*> -> visit(node)
		is DType<*> -> visit(node)
		else -> TODO("Unsupported $node")
	}

	open fun visit(node: DRef<*>): Unit = when (node) {
		is DFieldAccess<*, *> -> visit(node)
		is DLocal<*> -> visit(node)
		else -> TODO("Unsupported $node")
	}

	open fun visit(node: DExpr<*>): Unit = when (node) {
		is DLiteral<*> -> visit(node)
		is DLocal<*> -> visit(node)
		is DArg<*> -> visit(node)
		is DUnopBool -> visit(node)
		is DBinopInt -> visit(node)
		is DBinopFloat -> visit(node)
		is DBinopIntBool -> visit(node)
		is DBinopFloatBool -> visit(node)
		is DExprInvoke<*, *> -> visit(node)
		is DFieldAccess<*, *> -> visit(node)
		else -> TODO("Unsupported $node")
	}

	open fun visit(node: DFieldAccess<*, *>): Unit {
		visit(node.clazz)
		visit(node.prop)
		visit(node.obj)
	}

	open fun visit(node: DExprInvoke<*, *>): Unit {
		visit(node.clazz)
		for (arg in node.args) visit(arg)
	}

	open fun visit(node: DUnopBool): Unit {
		visit(node.right)
	}

	open fun visit(node: DBinopInt): Unit {
		visit(node.left)
		visit(node.right)
	}

	open fun visit(node: DBinopFloat): Unit {
		visit(node.left)
		visit(node.right)
	}

	open fun visit(node: DBinopIntBool): Unit {
		visit(node.left)
		visit(node.right)
	}

	open fun visit(node: DBinopFloatBool): Unit {
		visit(node.left)
		visit(node.right)
	}

	open fun visit(node: DLiteral<*>): Unit {
	}

	open fun visit(node: DArg<*>): Unit {
		visit(node.clazz)
	}

	open fun visit(node: DStm): Unit = when (node) {
		is DStms -> visit(node)
		is DIfElse -> visit(node)
		is DWhile -> visit(node)
		is DAssign<*> -> visit(node)
		is DStmExpr -> visit(node)
		is DReturnVoid -> visit(node)
		else -> TODO("Unsupported $node")
	}

	open fun visit(node: DStmExpr): Unit {
		visit(node.expr)
	}

	open fun visit(node: DReturnVoid): Unit {
	}

	open fun visit(node: DAssign<*>): Unit {
		visit(node.left)
		visit(node.value)
	}

	open fun visit(node: DStms): Unit {
		for (stm in node.stms) visit(stm)
	}

	open fun visit(node: DIfElse): Unit {
		visit(node.cond)
		visit(node.strue)
		node.sfalse?.let { visit(it) }
	}

	open fun visit(node: DWhile): Unit {
		visit(node.cond)
		visit(node.block)
	}

	open fun visit(node: DType<*>): Unit = when (node) {
		is DClass<*> -> visit(node)
		is DPrimType<*> -> visit(node)
		else -> TODO("Unsupported $node")
	}

	open fun visit(node: DClass<*>): Unit {
		visit(node.clazz)
	}

	open fun visit(node: DPrimType<*>): Unit {
		visit(node.clazz)
	}

	open fun visit(node: DLocal<*>): Unit {
		visit(node.initialValue)
	}

	open fun visit(node: KClass<*>): Unit {
	}

	open fun visit(node: KMutableProperty1<*, *>): Unit {
	}
}

fun DFunction.getLocals(): LinkedHashSet<DLocal<*>> {
	val locals = LinkedHashSet<DLocal<*>>()
	object : DNodeVisitor() {
		override fun visit(node: DLocal<*>) {
			super.visit(node)
			locals += node
		}
	}.visit(this)
	return locals
}