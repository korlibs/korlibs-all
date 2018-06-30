package com.soywiz.kds

// https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/common/src/generated/_Collections.kt

// @TODO: kotlin-native bug with sortedBy, sortedByDescending: https://github.com/JetBrains/kotlin-native/issues/1745
fun <T, T2 : Comparable<T2>> Iterable<T>.sortedBy2(callback: (T) -> T2): List<T> {
	return sortedWith(Comparator { a, b -> callback(a).compareTo(callback(b)) })
}

// @TODO: kotlin-native bug with sortedBy, sortedByDescending: https://github.com/JetBrains/kotlin-native/issues/1745
fun <T, T2 : Comparable<T2>> Iterable<T>.sortedByDescending2(callback: (T) -> T2): List<T> {
	return sortedWith(Comparator { a, b -> -callback(a).compareTo(callback(b)) })
}