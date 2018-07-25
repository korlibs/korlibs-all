package com.dragonbones.util

class Either<T1, T2>(val value: Any?) {
	val isFirst get() = value is T1
	val isSecond get() = value is T2
	val first get() = value as T1
	val second get() = value as T2
}