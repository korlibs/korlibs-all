package com.soywiz.dynarek

operator fun DExpr<Float>.plus(that: DExpr<Float>): DExpr<Float> = DBinopFloat(this, FBinop.ADD, that)
operator fun DExpr<Float>.minus(that: DExpr<Float>): DExpr<Float> = DBinopFloat(this, FBinop.SUB, that)
operator fun DExpr<Float>.times(that: DExpr<Float>): DExpr<Float> = DBinopFloat(this, FBinop.MUL, that)
operator fun DExpr<Float>.div(that: DExpr<Float>): DExpr<Float> = DBinopFloat(this, FBinop.DIV, that)
operator fun DExpr<Float>.rem(that: DExpr<Float>): DExpr<Float> = DBinopFloat(this, FBinop.REM, that)
operator fun DExpr<Float>.unaryMinus(): DExpr<Float> = DBinopFloat(DLiteral(0f), FBinop.SUB, this)
operator fun DExpr<Float>.unaryPlus(): DExpr<Float> = this

infix fun DExpr<Float>.eq(that: DExpr<Float>) = DBinopFloatBool(this, Compop.EQ, that)
infix fun DExpr<Float>.ne(that: DExpr<Float>) = DBinopFloatBool(this, Compop.NE, that)
infix fun DExpr<Float>.ge(that: DExpr<Float>) = DBinopFloatBool(this, Compop.GE, that)
infix fun DExpr<Float>.le(that: DExpr<Float>) = DBinopFloatBool(this, Compop.LE, that)
infix fun DExpr<Float>.gt(that: DExpr<Float>) = DBinopFloatBool(this, Compop.GT, that)
infix fun DExpr<Float>.lt(that: DExpr<Float>) = DBinopFloatBool(this, Compop.LT, that)

