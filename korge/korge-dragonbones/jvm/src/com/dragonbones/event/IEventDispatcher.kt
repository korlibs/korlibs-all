package com.dragonbones.event

import java.util.function.Consumer

/**
 * 事件接口。
 *
 * @version DragonBones 4.5
 * @language zh_CN
 */
interface IEventDispatcher {
    /**
     * @private
     */
    fun _dispatchEvent(type: EventStringType, eventObject: EventObject)

    /**
     * 是否包含指定类型的事件。
     *
     * @param type 事件类型。
     * @version DragonBones 4.5
     * @language zh_CN
     */
    fun hasEvent(type: EventStringType): Boolean

    /**
     * 添加事件。
     *
     * @param type     事件类型。
     * @param listener 事件回调。
     * @version DragonBones 4.5
     * @language zh_CN
     */
    fun addEvent(type: EventStringType, listener: Consumer<Any>, target: Any)

    /**
     * 移除事件。
     *
     * @param type     事件类型。
     * @param listener 事件回调。
     * @version DragonBones 4.5
     * @language zh_CN
     */
    fun removeEvent(type: EventStringType, listener: Consumer<Any>, target: Any)
}
