package com.dragonbones.util

open class ShortArray : IntArray {
    constructor(none: Boolean) : super(none) {}

    constructor() {}

    constructor(length: Int) : super(length) {}

    constructor(data: IntArray) : super(data) {}

    constructor(data: IntArray, length: Int) : super(data, length) {}

    override fun createInstance(): IntArray {
        return ShortArray()
    }
}
