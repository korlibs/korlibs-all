package com.soywiz.korma

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

inline operator fun <T> Array2<T>.get(p: Point2d): T = get(p.x.toInt(), p.y.toInt())
inline operator fun <T> Array2<T>.set(p: Point2d, value: T) = set(p.x.toInt(), p.y.toInt(), value)

inline fun <T> Array2<T>.tryGet(p: Point2d): T? = tryGet(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.trySet(p: Point2d, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)

inline operator fun <T> Array2<T>.get(p: PointInt): T = get(p.x, p.y)
inline operator fun <T> Array2<T>.set(p: PointInt, value: T) = set(p.x, p.y, value)

inline fun <T> Array2<T>.tryGet(p: PointInt): T? = tryGet(p.x, p.y)
inline fun <T> Array2<T>.trySet(p: PointInt, value: T) = trySet(p.x, p.y, value)
