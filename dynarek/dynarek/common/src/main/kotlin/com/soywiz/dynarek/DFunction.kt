package com.soywiz.dynarek

interface DFunction {
	//(val ret: DType<*>, val args: List<DType<*>>, val body: DStm)
	val ret: DType<*>
	val args: List<DType<*>>
	val body: DStm
}

data class DFunction0<TRet : Any>(override val ret: DType<TRet>, override val body: DStm) : DFunction {
	override val args = listOf<DType<*>>()
}

data class DFunction1<TRet : Any, T0 : Any>(override val ret: DType<TRet>, val p0: DType<T0>, override val body: DStm) :
	DFunction {
	override val args = listOf<DType<*>>(p0)
}

data class DFunction2<TRet : Any, T0 : Any, T1 : Any>(
	override val ret: DType<TRet>,
	val p0: DType<T0>,
	val p1: DType<T1>,
	override val body: DStm
) : DFunction {
	override val args = listOf<DType<*>>(p0, p1)
}

data class DFunction3<TRet : Any, T0 : Any, T1 : Any, T2 : Any>(
	override val ret: DType<TRet>,
	val p0: DType<T0>,
	val p1: DType<T1>,
	val p2: DType<T2>,
	override val body: DStm
) : DFunction {
	override val args = listOf<DType<*>>(p0, p1, p2)
}

//fun <TRet> function(ret: DType<TRet>, vararg args: DType<*>, block: StmBuilder<TRet, Unit, Unit, Unit, Unit>.() -> Unit): DFunction {
//	val builder = StmBuilder<TRet, Unit, Unit, Unit, Unit>()
//	block(builder)
//	return DFunction(ret, args.toList(), builder.build())
//}

inline fun <reified TRet : Any> function(
	ret: DType<TRet>,
	block: StmBuilder<TRet, Unit, Unit>.() -> Unit
): DFunction0<TRet> {
	val builder = StmBuilder(TRet::class, Unit::class, Unit::class)
	block(builder)
	return DFunction0(ret, builder.build())
}

inline fun <reified TRet : Any, reified T0 : Any> function(
	arg0: DType<T0>,
	ret: DType<TRet>,
	block: StmBuilder<TRet, T0, Unit>.() -> Unit
): DFunction1<TRet, T0> {
	val builder = StmBuilder(TRet::class, T0::class, Unit::class)
	block(builder)
	return DFunction1(ret, arg0, builder.build())
}

inline fun <reified TRet : Any, reified T0 : Any, reified T1 : Any> function(
	arg0: DType<T0>,
	arg1: DType<T1>,
	ret: DType<TRet>,
	block: StmBuilder<TRet, T0, T1>.() -> Unit
): DFunction2<TRet, T0, T1> {
	val builder = StmBuilder(TRet::class, T0::class, T1::class)
	block(builder)
	return DFunction2(ret, arg0, arg1, builder.build())
}
