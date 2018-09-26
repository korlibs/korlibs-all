package com.soywiz.kds

abstract class NumberArrayList {
	abstract var size: Int
	abstract fun getDouble(index: Int): Double
	abstract fun setDouble(index: Int, value: Double): Unit
	open fun getInt(index: Int): Int = getDouble(index).toInt()
	open fun setInt(index: Int, value: Int): Unit = setDouble(index, value.toDouble())
}

@Deprecated("", ReplaceWith("getDouble(index)"))
operator fun NumberArrayList.get(index: Int) = getDouble(index)
@Deprecated("", ReplaceWith("setDouble(index, value)"))
operator fun NumberArrayList.set(index: Int, value: Double) = setDouble(index, value)
