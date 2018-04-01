package com.soywiz.korma.numeric

fun Int.nextAlignedTo(align: Int) = if (this % align == 0) {
	this
} else {
	(((this / align) + 1) * align)
}

fun Int.isAlignedTo(alignment: Int) = (this % alignment) == 0
