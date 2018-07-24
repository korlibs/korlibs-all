package com.dragonbones.util

var <T> ArrayList<T>.length
	get() = size
	set(value) {
		while (size < value) {
			@Suppress("UNCHECKED_CAST")
			(this as ArrayList<T?>).add(null)
		}
	}
