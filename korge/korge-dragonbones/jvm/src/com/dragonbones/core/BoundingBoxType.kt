package com.dragonbones.core

/**
 * @version DragonBones 5.0
 * @language zh_CN
 * 包围盒类型。
 */
enum class BoundingBoxType private constructor(val v: Int) {
    Rectangle(0),
    Ellipse(1),
    Polygon(2);


    companion object {

        var values = values()
    }
}
