package com.soywiz.korag.shader.gl

import com.soywiz.korag.shader.*
import com.soywiz.korio.error.*
import com.soywiz.korio.util.*

class GlslGenerator(val kind: ShaderType, @Suppress("unused") val gles: Boolean = true, val version: Int = 100) :
	Program.Visitor<String>("") {
	private val temps = hashSetOf<Temp>()
	private val attributes = hashSetOf<Attribute>()
	private val varyings = hashSetOf<Varying>()
	private val uniforms = hashSetOf<Uniform>()
	private var programIndenter = Indenter()

	private fun errorType(type: VarType): Nothing = invalidOp("Don't know how to serialize type $type")

	fun typeToString(type: VarType) = when (type) {
		VarType.Byte4 -> "vec4"
		VarType.Mat2 -> "mat2"
		VarType.Mat3 -> "mat3"
		VarType.Mat4 -> "mat4"
		VarType.TextureUnit -> "sampler2D"
		else -> {
			when (type.kind) {
				VarKind.BYTE, VarKind.UNSIGNED_BYTE, VarKind.SHORT, VarKind.UNSIGNED_SHORT, VarKind.FLOAT -> {
					when (type.elementCount) {
						1 -> "float"
						2 -> "vec2"
						3 -> "vec3"
						4 -> "vec4"
						else -> errorType(type)
					}
				}
				VarKind.INT -> {
					when (type.elementCount) {
						1 -> "int"
						2 -> "ivec2"
						3 -> "ivec3"
						4 -> "ivec4"
						else -> errorType(type)
					}
				}
			}
		}
	}

	fun generate(root: Program.Stm): String {
		temps.clear()
		attributes.clear()
		varyings.clear()
		uniforms.clear()
		programIndenter = Indenter()
		visit(root)

		if (kind == ShaderType.FRAGMENT && attributes.isNotEmpty()) {
			throw RuntimeException("Can't use attributes in fragment shader")
		}

		return Indenter {
			if (gles) {
				line("#version $version")
				line("#ifdef GL_ES")
				indent {
					line("precision mediump float;")
					line("precision mediump int;")
					line("precision lowp sampler2D;")
					line("precision lowp samplerCube;")
				}
				line("#endif")
			}

			for (it in attributes) line("attribute ${typeToString(it.type)} ${it.name};")
			for (it in uniforms) line("uniform ${typeToString(it.type)} ${it.name};")
			for (it in varyings) line("varying ${typeToString(it.type)} ${it.name};")

			line("void main()") {
				for (temp in temps) {
					line(typeToString(temp.type) + " " + temp.name + ";")
				}
				line(programIndenter)
			}
		}.toString()

	}

	override fun visit(stms: Program.Stm.Stms) {
		//programIndenter.line("") {
			for (stm in stms.stms) {
				visit(stm)
			}
		//}
	}

	override fun visit(stm: Program.Stm.Set) {
		programIndenter.line("${visit(stm.to)} = ${visit(stm.from)};")
	}

	override fun visit(stm: Program.Stm.Discard) {
		programIndenter.line("discard;")
	}

	override fun visit(operand: Program.Vector): String =
		typeToString(operand.type) + "(" + operand.ops.joinToString(", ") { visit(it) } + ")"

	override fun visit(operand: Program.Binop): String = "(" + visit(operand.left) + " " + operand.op + " " + visit(operand.right) + ")"
	override fun visit(func: Program.Func): String = func.name + "(" + func.ops.joinToString(", ") { visit(it) } + ")"

	override fun visit(stm: Program.Stm.If) {
		programIndenter.apply {
			line("if (${visit(stm.cond)})") {
				visit(stm.tbody)
			}
			if (stm.fbody != null) {
				line("else") {
					visit(stm.fbody!!)
				}
			}
		}
	}

	override fun visit(operand: Variable): String {
		super.visit(operand)
		return when (operand) {
			is Output -> when (kind) {
				ShaderType.VERTEX -> "gl_Position"
				ShaderType.FRAGMENT -> "gl_FragColor"
			}
			else -> operand.name
		}
	}

	override fun visit(temp: Temp): String {
		temps += temp
		return super.visit(temp)
	}

	override fun visit(attribute: Attribute): String {
		attributes += attribute
		return super.visit(attribute)
	}

	override fun visit(varying: Varying): String {
		varyings += varying
		return super.visit(varying)
	}

	override fun visit(uniform: Uniform): String {
		uniforms += uniform
		return super.visit(uniform)
	}

	override fun visit(output: Output): String {
		return super.visit(output)
	}

	override fun visit(operand: Program.IntLiteral): String = "${operand.value}"

	override fun visit(operand: Program.FloatLiteral): String {
		val str = "${operand.value}"
		return if (str.contains('.')) str else "$str.0"
	}

	override fun visit(operand: Program.BoolLiteral): String = "${operand.value}"
	override fun visit(operand: Program.Swizzle): String = visit(operand.left) + "." + operand.swizzle
	override fun visit(operand: Program.ArrayAccess): String = visit(operand.left) + "[" + visit(operand.index) + "]"
}

fun Shader.toGlSl(): String = GlslGenerator(this.type).generate(this.stm)