package com.soywiz.dynarek

operator fun DExpr<Int>.plus(that: DExpr<Int>): DExpr<Int> = DBinopInt(this, IBinop.ADD, that)
operator fun DExpr<Int>.minus(that: DExpr<Int>): DExpr<Int> = DBinopInt(this, IBinop.SUB, that)
operator fun DExpr<Int>.times(that: DExpr<Int>): DExpr<Int> = DBinopInt(this, IBinop.MUL, that)
operator fun DExpr<Int>.div(that: DExpr<Int>): DExpr<Int> = DBinopInt(this, IBinop.DIV, that)
operator fun DExpr<Int>.rem(that: DExpr<Int>): DExpr<Int> = DBinopInt(this, IBinop.REM, that)
operator fun DExpr<Int>.unaryMinus(): DExpr<Int> = DBinopInt(DLiteral(0), IBinop.SUB, this)
operator fun DExpr<Int>.unaryPlus(): DExpr<Int> = this

infix fun DExpr<Int>.and(that: DExpr<Int>) = DBinopInt(this, IBinop.AND, that)
infix fun DExpr<Int>.or(that: DExpr<Int>) = DBinopInt(this, IBinop.OR, that)
infix fun DExpr<Int>.xor(that: DExpr<Int>) = DBinopInt(this, IBinop.XOR, that)
infix fun DExpr<Int>.shl(that: DExpr<Int>) = DBinopInt(this, IBinop.SHL, that)
infix fun DExpr<Int>.shr(that: DExpr<Int>) = DBinopInt(this, IBinop.SHR, that)
infix fun DExpr<Int>.ushr(that: DExpr<Int>) = DBinopInt(this, IBinop.USHR, that)
fun DExpr<Int>.inv() = DBinopInt(this, IBinop.XOR, (DLiteral(-1)))

infix fun DExpr<Int>.eq(that: DExpr<Int>) = DBinopIntBool(this, Compop.EQ, that)
infix fun DExpr<Int>.ne(that: DExpr<Int>) = DBinopIntBool(this, Compop.NE, that)
infix fun DExpr<Int>.ge(that: DExpr<Int>) = DBinopIntBool(this, Compop.GE, that)
infix fun DExpr<Int>.le(that: DExpr<Int>) = DBinopIntBool(this, Compop.LE, that)
infix fun DExpr<Int>.gt(that: DExpr<Int>) = DBinopIntBool(this, Compop.GT, that)
infix fun DExpr<Int>.lt(that: DExpr<Int>) = DBinopIntBool(this, Compop.LT, that)

