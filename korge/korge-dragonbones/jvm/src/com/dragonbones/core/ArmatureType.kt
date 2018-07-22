package com.dragonbones.core

/**
 * @private
 */
enum class ArmatureType private constructor(val v: Int) {
    Armature(0),
    MovieClip(1),
    Stage(2);


    companion object {

        var values = values()
    }
}
