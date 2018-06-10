package com.soywiz.dynarek

import kotlin.reflect.*

class StmBuilder<TRet : Any, T0 : Any, T1 : Any>(val ret: KClass<TRet>, val t0: KClass<T0>, val t1: KClass<T1>) {
	inner class ElseBuilder(val ifElse: DIfElse) {
		infix fun ELSE(block: StmBuilder<TRet, T0, T1>.() -> Unit) {
			val b = createBuilder()
			block(b)
			ifElse.sfalse = b.build()
		}
	}

	val stms = ArrayList<DStm>()

	fun createBuilder() = StmBuilder<TRet, T0, T1>(ret, t0, t1)

	val <T> T.lit: DLiteral<T> get() = DLiteral(this)
	val Int.litHex: DLiteral<Int> get() = DLiteral(this, kind = "hex")

	val p0 get() = DArg<T0>(t0, 0)
	val p1 get() = DArg<T1>(t1, 1)

	inline operator fun <reified T : Any, TR> DExpr<T>.set(prop: KMutableProperty1<T, TR>, value: DExpr<TR>) {
		SET(DFieldAccess(T::class, this, prop), value)
	}

	inline operator fun <reified T : Any, TR> DExpr<T>.get(prop: KMutableProperty1<T, TR>): DFieldAccess<T, TR> =
		DFieldAccess(T::class, this, prop)

	//inline operator fun <reified T : Any, TR> DExpr<T>.get(func: KFunction1<T, TR>): DInstanceMethod1<T, TR> = DInstanceMethod1(T::class, this, func)

	fun RET(expr: DExpr<TRet>) = stms.add(DReturnExpr(expr))
	fun RET() = stms.add(DReturnVoid(true))
	fun <T> SET(ref: DRef<T>, value: DExpr<T>) = stms.add(DAssign(ref, value))

	fun <T> STM(expr: DExpr<T>) = stms.add(DStmExpr(expr))
	fun STM(stm: DStm) = stms.add(stm)

	inline operator fun <reified TThis : Any, TR : Any> KFunction1<TThis, TR>.invoke(p0: DExpr<TThis>): DExpr<TR> =
		DExprInvoke1(TThis::class, this, p0)

	inline operator fun <reified TThis : Any, T1 : Any, TR : Any> KFunction2<TThis, T1, TR>.invoke(
		p0: DExpr<TThis>,
		p1: DExpr<T1>
	): DExpr<TR> = DExprInvoke2(TThis::class, this, p0, p1)

	inline operator fun <reified TThis : Any, T1 : Any, T2 : Any, TR : Any> KFunction3<TThis, T1, T2, TR>.invoke(
		p0: DExpr<TThis>,
		p1: DExpr<T1>,
		p2: DExpr<T2>
	): DExpr<TR> = DExprInvoke3(TThis::class, this, p0, p1, p2)

	inline operator fun <reified TThis : Any, T1 : Any, T2 : Any, T3 : Any, TR : Any> KFunction4<TThis, T1, T2, T3, TR>.invoke(
		p0: DExpr<TThis>,
		p1: DExpr<T1>,
		p2: DExpr<T2>,
		p3: DExpr<T3>
	): DExpr<TR> = DExprInvoke4(TThis::class, this, p0, p1, p2, p3)

	inline operator fun <reified TThis : Any, T1 : Any, T2 : Any, T3 : Any, T4 : Any, TR : Any> KFunction5<TThis, T1, T2, T3, T4, TR>.invoke(
		p0: DExpr<TThis>,
		p1: DExpr<T1>,
		p2: DExpr<T2>,
		p3: DExpr<T3>,
		p4: DExpr<T4>
	): DExpr<TR> = DExprInvoke5(TThis::class, this, p0, p1, p2, p3, p4)

	fun (StmBuilder<TRet, T0, T1>.() -> Unit).build(): DStm {
		val builder = createBuilder()
		this(builder)
		return builder.build()
	}

	fun IF(cond: Boolean, block: StmBuilder<TRet, T0, T1>.() -> Unit): ElseBuilder = IF(cond.lit, block)

	fun IF(cond: DExpr<Boolean>, block: StmBuilder<TRet, T0, T1>.() -> Unit): ElseBuilder {
		val trueBuilder = createBuilder()
		block(trueBuilder)
		val ifElse = DIfElse(cond, trueBuilder.build())
		stms.add(ifElse)
		return ElseBuilder(ifElse)
	}

	fun WHILE(cond: DExpr<Boolean>, block: StmBuilder<TRet, T0, T1>.() -> Unit): Unit {
		stms.add(DWhile(cond, block.build()))
	}

	fun FOR(local: DLocal<Int>, start: DExpr<Int>, end: DExpr<Int>, block: StmBuilder<TRet, T0, T1>.() -> Unit): Unit {
		SET(local, start)
		WHILE(local lt end) {
			block()
			SET(local, local + 1.lit)
		}
	}

	fun build(): DStm = DStms(stms.toList())
}

//typealias StmBuilderBlock<TRet, T0, T1> = StmBuilder<TRet, T0, T1>.() -> Unit
