package com.dragonbones.core

/**
 * @private
 */
enum class DisplayType private constructor(val v: Int) {
    Image(0),
    Armature(1),
    Mesh(2),
    BoundingBox(3);


    companion object {

        var values = values()
    }
}
