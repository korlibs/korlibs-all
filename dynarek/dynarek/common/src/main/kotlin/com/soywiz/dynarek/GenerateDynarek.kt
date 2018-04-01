package com.soywiz.dynarek

expect annotation class JvmField()
expect annotation class JsName(val name: String)
expect fun <TRet : Any> DFunction0<TRet>.generateDynarek(): () -> TRet
expect fun <TRet : Any, T0 : Any> DFunction1<TRet, T0>.generateDynarek(): (T0) -> TRet
expect fun <TRet : Any, T0 : Any, T1 : Any> DFunction2<TRet, T0, T1>.generateDynarek(): (T0, T1) -> TRet

