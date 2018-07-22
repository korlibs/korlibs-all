package com.dragonbones.core

import com.dragonbones.animation.WorldClock
import com.dragonbones.armature.Armature
import com.dragonbones.event.EventObject
import com.dragonbones.event.IEventDispatcher
import com.dragonbones.util.Array

/**
 * @private
 */
class DragonBones(val eventManager: IEventDispatcher) {

    val clock = WorldClock()
    private val _events = Array<EventObject>()
    private val _objects = Array<BaseObject>()

	fun advanceTime(passedTime: Float) {
        if (this._objects.size() > 0) {
            for (`object` in this._objects) {
                `object`.returnToPool()
            }

            this._objects.clear()
        }

        this.clock.advanceTime(passedTime)

        if (this._events.size() > 0) {
            for (i in 0 until this._events.size()) {
                val eventObject = this._events.get(i)
                val armature = eventObject.armature

                armature!!.eventDispatcher!!._dispatchEvent(eventObject.type, eventObject)
                if (eventObject.type == EventObject.SOUND_EVENT) {
                    this.eventManager!!._dispatchEvent(eventObject.type, eventObject)
                }

                this.bufferObject(eventObject)
            }

            this._events.clear()
        }
    }

    fun bufferEvent(value: EventObject) {
        if (this._events.indexOf(value) < 0) {
            this._events.add(value)
        }
    }

    fun bufferObject(`object`: BaseObject) {
        if (this._objects.indexOf(`object`) < 0) {
            this._objects.add(`object`)
        }
    }

    companion object {
        var yDown = true
        var debug = false
        var debugDraw = false
        var VERSION = "5.1f"
    }
}
