package com.soywiz.dynarek

fun DExpr<Boolean>.not() = DUnopBool(BUnop.NOT, this)
