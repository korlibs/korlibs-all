/**
 * https://www.khronos.org/files/webgl/webgl-reference-card-1_0.pdf
 */

@file:Suppress("unused")

package com.soywiz.korag.shader

import com.soywiz.kmem.*
import com.soywiz.korio.error.*
import com.soywiz.korio.lang.*

enum class VarKind(val bytesSize: Int) {
	BYTE(1), UNSIGNED_BYTE(1), SHORT(2), UNSIGNED_SHORT(2), INT(4), FLOAT(4)
}

enum class VarType(val kind: VarKind, val elementCount: Int, val isMatrix: Boolean = false) {
	VOID(VarKind.BYTE, elementCount = 0),

	Mat2(VarKind.FLOAT, elementCount = 4, isMatrix = true),
	Mat3(VarKind.FLOAT, elementCount = 9, isMatrix = true),
	Mat4(VarKind.FLOAT, elementCount = 16, isMatrix = true),

	TextureUnit(VarKind.INT, elementCount = 1),

	Int1(VarKind.INT, elementCount = 1),

	Float1(VarKind.FLOAT, elementCount = 1),
	Float2(VarKind.FLOAT, elementCount = 2),
	Float3(VarKind.FLOAT, elementCount = 3),
	Float4(VarKind.FLOAT, elementCount = 4),

	Short1(VarKind.SHORT, elementCount = 1),
	Short2(VarKind.SHORT, elementCount = 2),
	Short3(VarKind.SHORT, elementCount = 3),
	Short4(VarKind.SHORT, elementCount = 4),

	Bool1(VarKind.UNSIGNED_BYTE, elementCount = 1),

	Byte4(VarKind.UNSIGNED_BYTE, elementCount = 4), // OLD: Is this right?

	SByte1(VarKind.BYTE, elementCount = 1),
	SByte2(VarKind.BYTE, elementCount = 2),
	SByte3(VarKind.BYTE, elementCount = 3),
	SByte4(VarKind.BYTE, elementCount = 4),

	UByte1(VarKind.UNSIGNED_BYTE, elementCount = 1),
	UByte2(VarKind.UNSIGNED_BYTE, elementCount = 2),
	UByte3(VarKind.UNSIGNED_BYTE, elementCount = 3),
	UByte4(VarKind.UNSIGNED_BYTE, elementCount = 4),

	SShort1(VarKind.SHORT, elementCount = 1),
	SShort2(VarKind.SHORT, elementCount = 2),
	SShort3(VarKind.SHORT, elementCount = 3),
	SShort4(VarKind.SHORT, elementCount = 4),

	UShort1(VarKind.UNSIGNED_SHORT, elementCount = 1),
	UShort2(VarKind.UNSIGNED_SHORT, elementCount = 2),
	UShort3(VarKind.UNSIGNED_SHORT, elementCount = 3),
	UShort4(VarKind.UNSIGNED_SHORT, elementCount = 4),

	SInt1(VarKind.INT, elementCount = 1),
	SInt2(VarKind.INT, elementCount = 2),
	SInt3(VarKind.INT, elementCount = 3),
	SInt4(VarKind.INT, elementCount = 4),
	;

	val bytesSize: Int = kind.bytesSize * elementCount

	companion object {
		fun BYTE(count: Int) =
			when (count) { 0 -> VOID; 1 -> SByte1; 2 -> SByte2; 3 -> SByte3; 4 -> SByte4; else -> invalidOp; }

		fun UBYTE(count: Int) =
			when (count) { 0 -> VOID; 1 -> UByte1; 2 -> UByte2; 3 -> UByte3; 4 -> UByte4; else -> invalidOp; }

		fun SHORT(count: Int) =
			when (count) { 0 -> VOID; 1 -> SShort1; 2 -> SShort2; 3 -> SShort3; 4 -> SShort4; else -> invalidOp; }

		fun USHORT(count: Int) =
			when (count) { 0 -> VOID; 1 -> UShort1; 2 -> UShort2; 3 -> UShort3; 4 -> UShort4; else -> invalidOp; }

		fun INT(count: Int) =
			when (count) { 0 -> VOID; 1 -> SInt1; 2 -> SInt2; 3 -> SInt3; 4 -> SInt4; else -> invalidOp; }

		fun FLOAT(count: Int) =
			when (count) { 0 -> VOID; 1 -> Float1; 2 -> Float2; 3 -> Float3; 4 -> Float4; else -> invalidOp; }
	}

}

//val out_Position = Output("gl_Position", VarType.Float4)
//val out_FragColor = Output("gl_FragColor", VarType.Float4)

enum class ShaderType {
	VERTEX, FRAGMENT
}

open class Operand(val type: VarType) {
}

open class Variable(val name: String, type: VarType) : Operand(type) {
	var id: Int = 0
	var data: Any? = null
}

open class Attribute(
	name: String,
	type: VarType,
	val normalized: Boolean,
	val offset: Int? = null,
	val active: Boolean = true
) : Variable(name, type) {
	constructor(name: String, type: VarType, normalized: Boolean) : this(name, type, normalized, null, true)

	fun inactived() = Attribute(name, type, normalized, offset = null, active = false)
	override fun toString(): String = "Attribute($name)"
}

open class Varying(name: String, type: VarType) : Variable(name, type) {
	override fun toString(): String = "Varying($name)"
}

open class Uniform(name: String, type: VarType) : Variable(name, type) {
	//companion object {
	//	var lastUid = 0
	//}
	//
	//val uid = lastUid++
	override fun toString(): String = "Uniform($name)"
}

open class Temp(id: Int, type: VarType) : Variable("temp$id", type) {
	override fun toString(): String = "Temp($name)"
}

object Output : Variable("out", VarType.Float4) {
	override fun toString(): String = "Output"
}

class Program(val vertex: VertexShader, val fragment: FragmentShader, val name: String = "program") : Closeable {
	val uniforms by lazy { vertex.uniforms + fragment.uniforms }
	val attributes by lazy { vertex.attributes + fragment.attributes }

	override fun close() {
	}

	override fun toString(): String =
		"Program(name=$name, attributes=${attributes.map { it.name }}, uniforms=${uniforms.map { it.name }})"

	class Binop(val left: Operand, val op: String, val right: Operand) : Operand(left.type)
	class IntLiteral(val value: Int) : Operand(VarType.Int1)
	class FloatLiteral(val value: Float) : Operand(VarType.Float1)
	class BoolLiteral(val value: Boolean) : Operand(VarType.Bool1)
	class Vector(type: VarType, val ops: List<Operand>) : Operand(type)
	class Swizzle(val left: Operand, val swizzle: String) : Operand(left.type)
	class ArrayAccess(val left: Operand, val index: Operand) : Operand(left.type)

	class Func(val name: String, val ops: List<Operand>) : Operand(VarType.Float1) {
		constructor(name: String, vararg ops: Operand) : this(name, ops.toList())
	}

	sealed class Stm {
		class Stms(val stms: List<Stm>) : Stm()
		class Set(val to: Operand, val from: Operand) : Stm()
		class Discard : Stm()
		class If(val cond: Operand, val tbody: Stm, var fbody: Stm? = null) : Stm()
	}

	// http://mew.cx/glsl_quickref.pdf
	class Builder(val type: ShaderType) {
		val outputStms = arrayListOf<Stm>()

		//inner class BuildIf(val stmIf: Stm.If) {
		//	fun ELSEIF(cond: Operand, callback: Builder.() -> Unit): BuildIf {
		//		//val body = Builder(type)
		//		//body.callback()
		//		//outputStms += Stm.If(cond, Stm.Stms(body.outputStms))
		//		TODO()
		//	}
//
		//	infix fun ELSE(callback: Builder.() -> Unit) {
		//		//val body = Builder(type)
		//		//body.callback()
		//		//outputStms += Stm.If(cond, Stm.Stms(body.outputStms))
		//		TODO()
		//	}
		//}

		infix fun Stm.If.ELSE(callback: Builder.() -> Unit) {
			val body = Builder(type)
			body.callback()
			this.fbody = Stm.Stms(body.outputStms)
		}

		inline fun IF(cond: Any, callback: Builder.() -> Unit): Stm.If {
			val body = Builder(type)
			body.callback()
			val stmIf = Stm.If(cond.op, Stm.Stms(body.outputStms))
			outputStms += stmIf
			return stmIf
		}

		fun SET(target: Operand, expr: Any) {
			outputStms += Stm.Set(target, expr.op)
		}

		fun DISCARD() {
			outputStms += Stm.Discard()
		}

		private var tempLastId = 3
		fun createTemp(type: VarType) = Temp(tempLastId++, type)

		infix fun Operand.set(from: Any) = run { outputStms += Stm.Set(this, from.op) }
		infix fun Operand.setTo(from: Any) = run { outputStms += Stm.Set(this, from.op) }

		fun Operand.assign(from: Any) {
			outputStms += Stm.Set(this, from.op)
		}

		//infix fun Operand.set(to: Operand) = Stm.Set(this, to)
		val out: Output = Output
		//fun out(to: Operand) = Stm.Set(if (type == ShaderType.VERTEX) out_Position else out_FragColor, to)

		fun sin(arg: Any) = Func("sin", arg.op)
		fun cos(arg: Any) = Func("cos", arg.op)
		fun tan(arg: Any) = Func("tan", arg.op)

		fun asin(arg: Any) = Func("asin", arg.op)
		fun acos(arg: Any) = Func("acos", arg.op)
		fun atan(arg: Any) = Func("atan", arg.op)

		fun radians(arg: Any) = Func("radians", arg.op)
		fun degrees(arg: Any) = Func("degrees", arg.op)

		// Sampling
		fun texture2D(a: Any, b: Any) = Func("texture2D", a.op, b.op)

		fun func(name: String, vararg args: Any) = Func(name, *args.map { it.op }.toTypedArray())

		fun pow(b: Any, e: Any) = Func("pow", b.op, e.op)
		fun exp(v: Any) = Func("exp", v.op)
		fun exp2(v: Any) = Func("exp2", v.op)
		fun log(v: Any) = Func("log", v.op)
		fun log2(v: Any) = Func("log2", v.op)
		fun sqrt(v: Any) = Func("sqrt", v.op)
		fun inversesqrt(v: Any) = Func("inversesqrt", v.op)

		fun abs(v: Any) = Func("abs", v.op)
		fun sign(v: Any) = Func("sign", v.op)
		fun ceil(v: Any) = Func("ceil", v.op)
		fun floor(v: Any) = Func("floor", v.op)
		fun fract(v: Any) = Func("fract", v.op)
		fun clamp(v: Any, min: Any, max: Any) = Func("clamp", v.op, min.op, max.op)
		fun min(a: Any, b: Any) = Func("min", a.op, b.op)
		fun max(a: Any, b: Any) = Func("max", a.op, b.op)
		fun mod(a: Any, b: Any) = Func("mod", a.op, b.op)
		fun mix(a: Any, b: Any, step: Any) = Func("mix", a.op, b.op, step.op)
		fun step(a: Any, b: Any) = Func("step", a.op, b.op)
		fun smoothstep(a: Any, b: Any, c: Any) = Func("smoothstep", a.op, b.op, c.op)

		fun length(a: Any) = Func("length", a.op)
		fun distance(a: Any, b: Any) = Func("distance", a.op, b.op)
		fun dot(a: Any, b: Any) = Func("dot", a.op, b.op)
		fun cross(a: Any, b: Any) = Func("cross", a.op, b.op)
		fun normalize(a: Any) = Func("normalize", a.op)
		fun faceforward(a: Any, b: Any, c: Any) = Func("faceforward", a.op, b.op, c.op)
		fun reflect(a: Any, b: Any) = Func("reflect", a.op, b.op)
		fun refract(a: Any, b: Any, c: Any) = Func("refract", a.op, b.op, c.op)

		val Int.lit: IntLiteral get() = IntLiteral(this)
		val Double.lit: FloatLiteral get() = FloatLiteral(this.toFloat())
		val Float.lit: FloatLiteral get() = FloatLiteral(this)
		val Boolean.lit: BoolLiteral get() = BoolLiteral(this)
		val Number.lit: Operand get() = this.op
		fun lit(type: VarType, vararg ops: Any): Operand = Vector(type, ops.toList().ops)
		fun vec1(vararg ops: Any): Operand = Vector(VarType.Float1, ops.toList().ops)
		fun vec2(vararg ops: Any): Operand = Vector(VarType.Float2, ops.toList().ops)
		fun vec3(vararg ops: Any): Operand = Vector(VarType.Float3, ops.toList().ops)
		fun vec4(vararg ops: Any): Operand = Vector(VarType.Float4, ops.toList().ops)
		//fun Operand.swizzle(swizzle: String): Operand = Swizzle(this, swizzle)
		operator fun Operand.get(index: Int): Operand {
			return when {
				this.type.isMatrix -> ArrayAccess(this, index.lit)
				else -> when (index) {
					0 -> this.x
					1 -> this.y
					2 -> this.z
					3 -> this.w
					else -> error("Invalid index $index")
				}
			}
		}
		operator fun Operand.get(swizzle: String) = Swizzle(this, swizzle)
		val Operand.x get() = this["x"]
		val Operand.y get() = this["y"]
		val Operand.z get() = this["z"]
		val Operand.w get() = this["w"]

		val Operand.r get() = this["x"]
		val Operand.g get() = this["y"]
		val Operand.b get() = this["z"]
		val Operand.a get() = this["w"]

		val List<Any>.ops: List<Operand> get() = this.map { it.op }

		val Any.op: Operand get() = when (this) {
			is Int -> this.lit
			is Float -> this.lit
			is Double -> this.lit
			is Boolean -> this.lit
			is Operand -> this
			else -> error("Unsupported $this as Operand for shaders")
		}

		operator fun Any.unaryMinus() = 0.0.lit - this

		operator fun Any.minus(that: Any) = Binop(this.op, "-", that.op)
		operator fun Any.plus(that: Any) = Binop(this.op, "+", that.op)
		operator fun Any.times(that: Any) = Binop(this.op, "*", that.op)
		operator fun Any.div(that: Any) = Binop(this.op, "/", that.op)
		operator fun Any.rem(that: Any) = Binop(this.op, "%", that.op)

		infix fun Any.eq(that: Any) = Binop(this.op, "==", that.op)
		infix fun Any.ne(that: Any) = Binop(this.op, "!=", that.op)
		infix fun Any.lt(that: Any) = Binop(this.op, "<", that.op)
		infix fun Any.le(that: Any) = Binop(this.op, "<=", that.op)
		infix fun Any.gt(that: Any) = Binop(this.op, ">", that.op)
		infix fun Any.ge(that: Any) = Binop(this.op, ">=", that.op)
	}

	open class Visitor<E>(val default: E) {
		protected open fun default(): E = default

		open fun visit(stm: Stm) = when (stm) {
			is Stm.Stms -> visit(stm)
			is Stm.Set -> visit(stm)
			is Stm.If -> visit(stm)
			is Stm.Discard -> visit(stm)
		}

		open fun visit(stms: Stm.Stms) {
			for (stm in stms.stms) visit(stm)
		}

		open fun visit(stm: Stm.If) {
			visit(stm.cond)
			visit(stm.tbody)
		}

		open fun visit(stm: Stm.Set) {
			visit(stm.from)
			visit(stm.to)
		}

		open fun visit(stm: Stm.Discard) {
		}

		open fun visit(operand: Operand): E = when (operand) {
			is Variable -> visit(operand)
			is Binop -> visit(operand)
			is BoolLiteral -> visit(operand)
			is IntLiteral -> visit(operand)
			is FloatLiteral -> visit(operand)
			is Vector -> visit(operand)
			is Swizzle -> visit(operand)
			is ArrayAccess -> visit(operand)
			is Func -> visit(operand)
			else -> invalidOp("Don't know how to visit operand $operand")
		}

		open fun visit(func: Func): E {
			for (op in func.ops) visit(op)
			return default()
		}

		open fun visit(operand: Variable): E = when (operand) {
			is Attribute -> visit(operand)
			is Varying -> visit(operand)
			is Uniform -> visit(operand)
			is Output -> visit(operand)
			is Temp -> visit(operand)
			else -> invalidOp("Don't know how to visit basename $operand")
		}

		open fun visit(temp: Temp): E = default()
		open fun visit(attribute: Attribute): E = default()
		open fun visit(varying: Varying): E = default()
		open fun visit(uniform: Uniform): E = default()
		open fun visit(output: Output): E = default()
		open fun visit(operand: Binop): E {
			visit(operand.left)
			visit(operand.right)
			return default()
		}

		open fun visit(operand: Swizzle): E {
			visit(operand.left)
			return default()
		}

		open fun visit(operand: ArrayAccess): E {
			visit(operand.left)
			visit(operand.index)
			return default()
		}

		open fun visit(operand: Vector): E {
			for (op in operand.ops) visit(op)
			return default()
		}

		open fun visit(operand: IntLiteral): E = default()
		open fun visit(operand: FloatLiteral): E = default()
		open fun visit(operand: BoolLiteral): E = default()
	}
}

open class Shader(val type: ShaderType, val stm: Program.Stm) {
	val uniforms by lazy {
		val out = LinkedHashSet<Uniform>()
		object : Program.Visitor<Unit>(Unit) {
			override fun visit(uniform: Uniform) = run { out += uniform }
		}.visit(stm)
		out.toSet()
	}

	val attributes by lazy {
		val out = LinkedHashSet<Attribute>()
		object : Program.Visitor<Unit>(Unit) {
			override fun visit(attribute: Attribute) = run { out += attribute }
		}.visit(stm)
		out.toSet()
	}
}

open class VertexShader(stm: Program.Stm) : Shader(ShaderType.VERTEX, stm)
open class FragmentShader(stm: Program.Stm) : Shader(ShaderType.FRAGMENT, stm)

fun FragmentShader.appending(callback: Program.Builder.() -> Unit): FragmentShader {
	return FragmentShader(Program.Stm.Stms(listOf(this.stm, FragmentShader(callback).stm)))
}

fun VertexShader(callback: Program.Builder.() -> Unit): VertexShader {
	val builder = Program.Builder(ShaderType.VERTEX)
	builder.callback()
	return VertexShader(Program.Stm.Stms(builder.outputStms))
}

fun FragmentShader(callback: Program.Builder.() -> Unit): FragmentShader {
	val builder = Program.Builder(ShaderType.FRAGMENT)
	builder.callback()
	return FragmentShader(Program.Stm.Stms(builder.outputStms))
}

class VertexLayout(attr: List<Attribute>, private val layoutSize: Int?) {
	private val myattr = attr
	val attributes = attr
	constructor(attributes: List<Attribute>) : this(attributes, null)
	constructor(vararg attributes: Attribute) : this(attributes.toList(), null)
	constructor(vararg attributes: Attribute, layoutSize: Int? = null) : this(attributes.toList(), layoutSize)

	private var _lastPos: Int = 0

	val alignments = myattr.map {
		val a = it.type.kind.bytesSize
		if (a <= 1) 1 else a
	}

	val attributePositions = myattr.map {
		if (it.offset != null) {
			_lastPos = it.offset
		} else {
			_lastPos = _lastPos.nextAlignedTo(it.type.kind.bytesSize)
		}
		val out = _lastPos
		_lastPos += it.type.bytesSize
		out
	}

	val maxAlignment = alignments.max() ?: 1
	val totalSize: Int = run { layoutSize ?: _lastPos.nextAlignedTo(maxAlignment) }

	override fun toString(): String = "VertexLayout[${myattr.map { it.name }.joinToString(", ")}]"
}
