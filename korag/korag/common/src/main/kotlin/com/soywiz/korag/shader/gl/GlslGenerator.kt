package com.soywiz.korag.shader.gl

import com.soywiz.korag.shader.*
import com.soywiz.korio.error.invalidOp

class GlslGenerator(val kind: ShaderType, @Suppress("unused") val gles: Boolean = true, val version: Int = 100) : Program.Visitor() {
	private val temps = hashSetOf<Temp>()
	private val attributes = hashSetOf<Attribute>()
	private val varyings = hashSetOf<Varying>()
	private val uniforms = hashSetOf<Uniform>()
	private var programStr = StringBuilder()

	private fun errorType(type: VarType): Nothing = invalidOp("Don't know how to serialize type $type")

	fun typeToString(type: VarType) = when (type) {
		VarType.Byte4 -> "vec4"
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
		programStr = StringBuilder()
		visit(root)

		val prefix = arrayListOf<String>()

		if (kind == ShaderType.FRAGMENT && attributes.isNotEmpty()) {
			throw RuntimeException("Can't use attributes in fragment shader")
		}
		for (a in attributes) prefix += "attribute ${typeToString(a.type)} ${a.name};"
		for (u in uniforms) prefix += "uniform ${typeToString(u.type)} ${u.name};"
		for (v in varyings) prefix += "varying ${typeToString(v.type)} ${v.name};"

		val precissions = arrayListOf<String>()
		precissions += "#version $version"
		precissions += "#ifdef GL_ES"
		precissions += "precision mediump float;"
		precissions += "precision mediump int;"
		precissions += "precision lowp sampler2D;"
		precissions += "precision lowp samplerCube;"
		precissions += "#endif"

		val tempsStr = temps.map {
			typeToString(it.type) + " " + it.name + ";"
		}

		val preprefix = if (gles) {
			precissions.joinToString("\n") + "\n"
		} else {
			""
		}

		return preprefix + prefix.joinToString("\n") + "\n" + "void main() {" + tempsStr.joinToString("\n") + programStr.toString() + "}"
	}

	override fun visit(stms: Program.Stm.Stms) {
		programStr.append("{")
		for (stm in stms.stms) visit(stm)
		programStr.append("}")
	}

	override fun visit(stm: Program.Stm.Set) {
		visit(stm.to)
		programStr.append(" = ")
		visit(stm.from)
		programStr.append(";")
	}

	override fun visit(stm: Program.Stm.Discard) {
		programStr.append("discard;")
	}

	override fun visit(operand: Program.Vector) {
		programStr.append("vec4(")
		var first = true
		for (op in operand.ops) {
			if (!first) {
				programStr.append(",")
			}
			visit(op)
			first = false
		}
		programStr.append(")")
	}

	override fun visit(operand: Program.Binop) {
		programStr.append("(")
		visit(operand.left)
		programStr.append(operand.op)
		visit(operand.right)
		programStr.append(")")
	}

	override fun visit(func: Program.Func) {
		programStr.append(func.name)
		programStr.append("(")
		var first = true
		for (op in func.ops) {
			if (!first) programStr.append(", ")
			visit(op)
			first = false
		}
		programStr.append(")")
	}

	override fun visit(stm: Program.Stm.If) {
		programStr.append("if (")
		visit(stm.cond)
		programStr.append(") ")
		visit(stm.tbody)
		if (stm.fbody != null) {
			programStr.append(" else ")
			visit(stm.fbody!!)
		}
	}

	override fun visit(operand: Variable) {
		if (operand is Output) {
			programStr.append(when (kind) {
				ShaderType.VERTEX -> "gl_Position"
				ShaderType.FRAGMENT -> "gl_FragColor"
			})
		} else {
			programStr.append(operand.name)
		}
		super.visit(operand)
	}

	override fun visit(temp: Temp) {
		temps += temp
		super.visit(temp)
	}

	override fun visit(attribute: Attribute) {
		attributes += attribute
		super.visit(attribute)
	}

	override fun visit(varying: Varying) {
		varyings += varying
		super.visit(varying)
	}

	override fun visit(uniform: Uniform) {
		uniforms += uniform
		super.visit(uniform)
	}

	override fun visit(output: Output) {
		super.visit(output)
	}

	override fun visit(operand: Program.IntLiteral) {
		programStr.append(operand.value)
		super.visit(operand)
	}

	override fun visit(operand: Program.FloatLiteral) {
		val str = "${operand.value}"

		if (str.contains('.')) {
			programStr.append(str)
		} else {
			programStr.append("$str.0")
		}
		super.visit(operand)
	}

	override fun visit(operand: Program.BoolLiteral) {
		programStr.append(operand.value)
		super.visit(operand)
	}

	override fun visit(operand: Program.Swizzle) {
		visit(operand.left)
		programStr.append(".${operand.swizzle}")
	}
}

fun Shader.toGlSl(): String = GlslGenerator(this.type).generate(this.stm)