package com.soywiz.korte

import com.soywiz.korio.error.noImpl
import com.soywiz.korio.reflect.ObjectMapper2
import com.soywiz.korio.util.quote
import com.soywiz.korio.util.toNumber
import kotlin.reflect.KClass
import kotlin.math.pow

//expect class DynamicBase {
//	//fun getFields(obj: Any?): List<String>
//	//fun getMethods(obj: Any?): List<String>
//	//fun invoke(obj: Any?, name: String, args: List<Any?>): Any?
//	//fun getFunctionArity(obj: Any?, name: String): Int
//	fun dynamicGet(obj: Any?, name: String): Any?
//	fun dynamicSet(obj: Any?, name: String, value: Any?): Unit
//}

object Dynamic2 {
	fun binop(l: Any?, r: Any?, op: String): Any? = when (op) {
		"+" -> {
			when (l) {
				is String -> l.toString() + toString(r)
				is Iterable<*> -> toIterable(l) + toIterable(r)
				else -> toDouble(l) + toDouble(r)
			}
		}
		"-" -> toDouble(l) - toDouble(r)
		"*" -> toDouble(l) * toDouble(r)
		"/" -> toDouble(l) / toDouble(r)
		"%" -> toDouble(l) % toDouble(r)
		"**" -> toDouble(l).pow(toDouble(r))
		"&" -> toInt(l) and toInt(r)
		"or" -> toInt(l) or toInt(r)
		"^" -> toInt(l) xor toInt(r)
		"&&" -> toBool(l) && toBool(r)
		"||" -> toBool(l) || toBool(r)
		"==" -> {
			if (l is Number && r is Number) {
				l.toDouble() == r.toDouble()
			} else {
				l == r
			}
		}
		"!=" -> {
			if (l is Number && r is Number) {
				l.toDouble() != r.toDouble()
			} else {
				l != r
			}
		}
		"<" -> compare(l, r) < 0
		"<=" -> compare(l, r) <= 0
		">" -> compare(l, r) > 0
		">=" -> compare(l, r) >= 0
		"in" -> contains(r, l)
		"?:" -> if (toBool(l)) l else r
		else -> noImpl("Not implemented binary operator '$op'")
	}

	fun unop(r: Any?, op: String): Any? = when (op) {
		"+" -> r
		"-" -> -toDouble(r)
		"~" -> toInt(r).inv()
		"!" -> !toBool(r)
		else -> noImpl("Not implemented unary operator $op")
	}

	fun contains(collection: Any?, element: Any?): Boolean = when (collection) {
		is Set<*> -> element in collection
		else -> element in toList(collection)
	}

	fun compare(l: Any?, r: Any?): Int {
		if (l is Number && r is Number) {
			return l.toDouble().compareTo(r.toDouble())
		}
		val lc = toComparable(l)
		val rc = toComparable(r)
		if (lc::class.isInstance(rc)) {
			return lc.compareTo(rc)
		} else {
			return -1
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun toComparable(it: Any?): Comparable<Any?> = when (it) {
		null -> 0 as Comparable<Any?>
		is Comparable<*> -> it as Comparable<Any?>
		else -> it.toString() as Comparable<Any?>
	}

	fun toBool(it: Any?): Boolean = when (it) {
		null -> false
		else -> toBoolOrNull(it) ?: true
	}

	fun toBoolOrNull(it: Any?): Boolean? = when (it) {
		null -> null
		is Boolean -> it
		is Number -> it.toDouble() != 0.0
		is String -> it.isNotEmpty() && it != "0" && it != "false"
		else -> null
	}

	fun toNumber(it: Any?): Number = when (it) {
		null -> 0.0
		is Number -> it
		else -> it.toString().toNumber()
	}

	fun toInt(it: Any?): Int = toNumber(it).toInt()
	fun toLong(it: Any?): Long = toNumber(it).toLong()
	fun toDouble(it: Any?): Double = toNumber(it).toDouble()

	fun toString(value: Any?): String = when (value) {
		null -> ""
		is String -> value
		is Double -> {
			if (value == value.toInt().toDouble()) {
				value.toInt().toString()
			} else {
				value.toString()
			}
		}
		is Iterable<*> -> "[" + value.map { toString(it) }.joinToString(", ") + "]"
		is Map<*, *> -> "{" + value.map { toString(it.key).quote() + ": " + toString(it.value) }.joinToString(", ") + "}"
		else -> value.toString()
	}

	fun length(subject: Any?): Int = when (subject) {
		null -> 0
		is Array<*> -> subject.size
		is List<*> -> subject.size
		is Map<*, *> -> subject.size
		is Iterable<*> -> subject.count()
		else -> subject.toString().length
	}

	fun toList(it: Any?): List<*> = toIterable(it).toList()

	fun toIterable(it: Any?): Iterable<*> = when (it) {
		null -> listOf<Any?>()
		is Iterable<*> -> it
		is CharSequence -> it.toList()
		is Map<*, *> -> it.toList()
		else -> listOf<Any?>()
	}

	suspend fun accessAny(instance: Any?, key: Any?, mapper: ObjectMapper2): Any? = when (instance) {
		null -> null
		is Map<*, *> -> instance[key]
		is Iterable<*> -> instance.toList()[toInt(key)]
		else -> {
			val keyStr = key.toDynamicString()
			when {
				mapper.hasProperty(instance, keyStr) -> {
					//println("Access dynamic property : $keyStr")
					mapper.get(instance, key)
				}
				mapper.hasMethod(instance, keyStr) -> {
					//println("Access dynamic method : $keyStr")
					mapper.invokeAsync(instance::class as KClass<Any>, instance as Any?, keyStr, listOf())
				}
				else -> {
					//println("Access dynamic null : '$keyStr'")
					null
				}
			}
		}
	}

	suspend fun setAny(instance: Any?, key: Any?, value: Any?, mapper: ObjectMapper2): Unit = when (instance) {
		null -> Unit
		is MutableMap<*, *> -> (instance as MutableMap<Any?, Any?>).set(key, value)
		is MutableList<*> -> (instance as MutableList<Any?>)[toInt(key)] = value
		else -> {
			when {
				mapper.hasProperty(instance, key.toDynamicString()) -> mapper.set(instance, key, value)
				mapper.hasMethod(instance, key.toDynamicString()) -> {
					mapper.invokeAsync(instance::class as KClass<Any>, instance as Any?, key.toDynamicString(), listOf(value))
					Unit
				}
				else -> Unit
			}
		}
	}

	suspend fun callAny(any: Any?, args: List<Any?>, mapper: ObjectMapper2): Any? = callAny(any, "invoke", args, mapper = mapper)
	suspend fun callAny(any: Any?, methodName: Any?, args: List<Any?>, mapper: ObjectMapper2): Any? = when (any) {
		null -> null
		else -> mapper.invokeAsync(any::class as KClass<Any>, any, methodName.toDynamicString(), args)
	}

	//fun dynamicCast(any: Any?, target: KClass<*>): Any? = TODO()
}

internal fun Any?.toDynamicString() = Dynamic2.toString(this)
internal fun Any?.toDynamicBool() = Dynamic2.toBool(this)
internal fun Any?.toDynamicInt() = Dynamic2.toInt(this)
internal fun Any?.toDynamicList() = Dynamic2.toList(this)
internal fun Any?.dynamicLength() = Dynamic2.length(this)
suspend internal fun Any?.dynamicGet(key: Any?, mapper: ObjectMapper2) = Dynamic2.accessAny(this, key, mapper)
suspend internal fun Any?.dynamicSet(key: Any?, value: Any?, mapper: ObjectMapper2) = Dynamic2.setAny(this, key, value, mapper)
suspend internal fun Any?.dynamicCall(vararg args: Any?, mapper: ObjectMapper2) = Dynamic2.callAny(this, args.toList(), mapper = mapper)
suspend internal fun Any?.dynamicCallMethod(methodName: Any?, vararg args: Any?, mapper: ObjectMapper2) = Dynamic2.callAny(this, methodName, args.toList(), mapper = mapper)
//suspend internal fun Any?.dynamicCastTo(target: KClass<*>) = Dynamic2.dynamicCast(this, target)

