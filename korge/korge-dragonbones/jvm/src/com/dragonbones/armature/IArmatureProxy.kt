package com.dragonbones.armature

import com.dragonbones.animation.Animation
import com.dragonbones.event.IEventDispatcher

/**
 * @version DragonBones 5.0
 * @language zh_CN
 * 骨架代理接口。
 */
interface IArmatureProxy : IEventDispatcher {

    /**
     * @language zh_CN
     * 获取骨架。
     * @version DragonBones 4.5
     * @see Armature
     */
    val armature: Armature

    /**
     * @language zh_CN
     * 获取动画控制器。
     * @version DragonBones 4.5
     * @see Animation
     */
    val animation: Animation

    /**
     * @private
     */
    fun init(armature: Armature)

    /**
     * @private
     */
    fun clear()

    /**
     * @language zh_CN
     * 释放代理和骨架。 (骨架会回收到对象池)
     * @version DragonBones 4.5
     */
    fun dispose(disposeProxy: Boolean)

    /**
     * @private
     */
    fun debugUpdate(isEnabled: Boolean)
}