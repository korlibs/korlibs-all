package com.dragonbones.factory

import com.dragonbones.model.ArmatureData
import com.dragonbones.model.DragonBonesData
import com.dragonbones.model.SkinData

/**
 * @private
 */
class BuildArmaturePackage {
    var dataName = ""
    var textureAtlasName = ""
    var data: DragonBonesData? = null
    var armature: ArmatureData? = null
    var skin: SkinData? = null
}
