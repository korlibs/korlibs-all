package com.soywiz.korma

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

inline fun <T> Array2<T>.get(p: Point2d): T = get(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.set(p: Point2d, value: T) = set(p.x.toInt(), p.y.toInt(), value)

inline fun <T> Array2<T>.tryGet(p: Point2d): T? = tryGet(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.trySet(p: Point2d, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
