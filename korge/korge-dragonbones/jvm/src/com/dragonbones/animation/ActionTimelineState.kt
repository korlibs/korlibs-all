package com.dragonbones.animation

import com.dragonbones.core.ActionType
import com.dragonbones.core.BaseObject
import com.dragonbones.core.BinaryOffset
import com.dragonbones.event.EventObject

/**
 * @internal
 * @private
 */
class ActionTimelineState : TimelineState() {
    private fun _onCrossFrame(frameIndex: Int) {
        val eventDispatcher = this._armature!!.eventDispatcher
        if (this._animationState!!.actionEnabled) {
            val frameOffset =
                this._animationData!!.frameOffset + this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset.v + frameIndex)
            val actionCount = this._frameArray!!.get(frameOffset + 1)
            val actions = this._armature!!.armatureData!!.actions
            for (i in 0 until actionCount) {
                val actionIndex = this._frameArray!!.get(frameOffset + 2 + i)
                val action = actions.get(actionIndex)
                if (action.type == ActionType.Play) {
                    if (action.slot != null) {
                        val slot = this._armature!!.getSlot(action.slot!!.name)
                        if (slot != null) {
                            val childArmature = slot.childArmature
                            childArmature?._bufferAction(action, true)
                        }
                    } else if (action.bone != null) {
                        for (slot in this._armature!!.slots) {
                            val childArmature = slot.childArmature
                            if (childArmature != null && slot.parent!!.boneData === action.bone) {
                                childArmature._bufferAction(action, true)
                            }
                        }
                    } else {
                        this._armature!!._bufferAction(action, true)
                    }
                } else {
                    val eventType =
                        if (action.type == ActionType.Frame) EventObject.FRAME_EVENT else EventObject.SOUND_EVENT
                    if (action.type == ActionType.Sound || eventDispatcher!!.hasEvent(eventType)) {
                        val eventObject = BaseObject.borrowObject(EventObject::class.java)
                        // eventObject.time = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
                        eventObject.time = this._frameArray!!.get(frameOffset) / this._frameRate
                        eventObject.type = eventType
                        eventObject.name = action.name
                        eventObject.data = action.data
                        eventObject.armature = this._armature
                        eventObject.animationState = this._animationState

                        if (action.bone != null) {
                            eventObject.bone = this._armature!!.getBone(action.bone!!.name)
                        }

                        if (action.slot != null) {
                            eventObject.slot = this._armature!!.getSlot(action.slot!!.name)
                        }

                        this._armature!!._dragonBones!!.bufferEvent(eventObject)
                    }
                }
            }
        }
    }

    override fun _onArriveAtFrame() {}

    override fun _onUpdateFrame() {}

    override fun update(passedTime: Float) {
        val prevState = this.playState
        var prevPlayTimes = this.currentPlayTimes.toFloat()
        val prevTime = this.mcurrentTime

        if (this.playState <= 0 && this._setCurrentTime(passedTime)) {
            val eventDispatcher = this._armature!!.eventDispatcher
            if (prevState < 0) {
                if (this.playState != prevState) {
                    if (this._animationState!!.displayControl && this._animationState!!.resetToPose) { // Reset zorder to pose.
                        this._armature!!._sortZOrder(null, 0)
                    }

                    prevPlayTimes = this.currentPlayTimes.toFloat()

                    if (eventDispatcher!!.hasEvent(EventObject.START)) {
                        val eventObject = BaseObject.borrowObject(EventObject::class.java)
                        eventObject.type = EventObject.START
                        eventObject.armature = this._armature
                        eventObject.animationState = this._animationState
                        this._armature!!._dragonBones!!.bufferEvent(eventObject)
                    }
                } else {
                    return
                }
            }

            val isReverse = this._animationState!!.timeScale < 0f
            var loopCompleteEvent: EventObject? = null
            var completeEvent: EventObject? = null
            if (this.currentPlayTimes.toFloat() != prevPlayTimes) {
                if (eventDispatcher!!.hasEvent(EventObject.LOOP_COMPLETE)) {
                    loopCompleteEvent = BaseObject.borrowObject(EventObject::class.java)
                    loopCompleteEvent!!.type = EventObject.LOOP_COMPLETE
                    loopCompleteEvent.armature = this._armature
                    loopCompleteEvent.animationState = this._animationState
                }

                if (this.playState > 0) {
                    if (eventDispatcher.hasEvent(EventObject.COMPLETE)) {
                        completeEvent = BaseObject.borrowObject(EventObject::class.java)
                        completeEvent!!.type = EventObject.COMPLETE
                        completeEvent.armature = this._armature
                        completeEvent.animationState = this._animationState
                    }

                }
            }

            if (this._frameCount > 1) {
                val timelineData = this._timelineData
                val timelineFrameIndex = Math.floor((this.mcurrentTime * this._frameRate).toDouble()).toInt() // uint
                val frameIndex = this._frameIndices!!.get(timelineData!!.frameIndicesOffset + timelineFrameIndex)
                if (this._frameIndex != frameIndex) { // Arrive at frame.
                    var crossedFrameIndex = this._frameIndex
                    this._frameIndex = frameIndex
                    if (this._timelineArray != null) {
                        this._frameOffset = this._animationData!!.frameOffset +
                                this._timelineArray!!.get(timelineData.offset + BinaryOffset.TimelineFrameOffset.v + this._frameIndex)
                        if (isReverse) {
                            if (crossedFrameIndex < 0) {
                                val prevFrameIndex = Math.floor((prevTime * this._frameRate).toDouble()).toInt()
                                crossedFrameIndex =
                                        this._frameIndices!!.get(timelineData.frameIndicesOffset + prevFrameIndex)
                                if (this.currentPlayTimes.toFloat() == prevPlayTimes) { // Start.
                                    if (crossedFrameIndex == frameIndex) { // Uncrossed.
                                        crossedFrameIndex = -1
                                    }
                                }
                            }

                            while (crossedFrameIndex >= 0) {
                                val frameOffset =
                                    this._animationData!!.frameOffset + this._timelineArray!!.get(timelineData.offset + BinaryOffset.TimelineFrameOffset.v + crossedFrameIndex)
                                // const framePosition = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
                                val framePosition = this._frameArray!!.get(frameOffset) / this._frameRate
                                if (this._position <= framePosition && framePosition <= this._position + this._duration) { // Support interval play.
                                    this._onCrossFrame(crossedFrameIndex)
                                }

                                if (loopCompleteEvent != null && crossedFrameIndex == 0) { // Add loop complete event after first frame.
                                    this._armature!!._dragonBones!!.bufferEvent(loopCompleteEvent)
                                    loopCompleteEvent = null
                                }

                                if (crossedFrameIndex > 0) {
                                    crossedFrameIndex--
                                } else {
                                    crossedFrameIndex = this._frameCount - 1
                                }

                                if (crossedFrameIndex == frameIndex) {
                                    break
                                }
                            }
                        } else {
                            if (crossedFrameIndex < 0) {
                                val prevFrameIndex = Math.floor((prevTime * this._frameRate).toDouble()).toInt()
                                crossedFrameIndex =
                                        this._frameIndices!!.get(timelineData.frameIndicesOffset + prevFrameIndex)
                                val frameOffset =
                                    this._animationData!!.frameOffset + this._timelineArray!!.get(timelineData.offset + BinaryOffset.TimelineFrameOffset.v + crossedFrameIndex)
                                // const framePosition = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
                                val framePosition = this._frameArray!!.get(frameOffset) / this._frameRate
                                if (this.currentPlayTimes.toFloat() == prevPlayTimes) { // Start.
                                    if (prevTime <= framePosition) { // Crossed.
                                        if (crossedFrameIndex > 0) {
                                            crossedFrameIndex--
                                        } else {
                                            crossedFrameIndex = this._frameCount - 1
                                        }
                                    } else if (crossedFrameIndex == frameIndex) { // Uncrossed.
                                        crossedFrameIndex = -1
                                    }
                                }
                            }

                            while (crossedFrameIndex >= 0) {
                                if (crossedFrameIndex < this._frameCount - 1) {
                                    crossedFrameIndex++
                                } else {
                                    crossedFrameIndex = 0
                                }

                                val frameOffset =
                                    this._animationData!!.frameOffset + this._timelineArray!!.get(timelineData.offset + BinaryOffset.TimelineFrameOffset.v + crossedFrameIndex)
                                // const framePosition = this._frameArray[frameOffset] * this._frameRateR; // Precision problem
                                val framePosition = this._frameArray!!.get(frameOffset) / this._frameRate
                                if (this._position <= framePosition && framePosition <= this._position + this._duration) { // Support interval play.
                                    this._onCrossFrame(crossedFrameIndex)
                                }

                                if (loopCompleteEvent != null && crossedFrameIndex == 0) { // Add loop complete event before first frame.
                                    this._armature!!._dragonBones!!.bufferEvent(loopCompleteEvent)
                                    loopCompleteEvent = null
                                }

                                if (crossedFrameIndex == frameIndex) {
                                    break
                                }
                            }
                        }
                    }
                }
            } else if (this._frameIndex < 0) {
                this._frameIndex = 0
                if (this._timelineData != null) {
                    this._frameOffset = this._animationData!!.frameOffset +
                            this._timelineArray!!.get(this._timelineData!!.offset + BinaryOffset.TimelineFrameOffset.v)
                    // Arrive at frame.
                    val framePosition = this._frameArray!!.get(this._frameOffset) / this._frameRate
                    if (this.currentPlayTimes.toFloat() == prevPlayTimes) { // Start.
                        if (prevTime <= framePosition) {
                            this._onCrossFrame(this._frameIndex)
                        }
                    } else if (this._position <= framePosition) { // Loop complete.
                        if (!isReverse && loopCompleteEvent != null) { // Add loop complete event before first frame.
                            this._armature!!._dragonBones!!.bufferEvent(loopCompleteEvent)
                            loopCompleteEvent = null
                        }

                        this._onCrossFrame(this._frameIndex)
                    }
                }
            }

            if (loopCompleteEvent != null) {
                this._armature!!._dragonBones!!.bufferEvent(loopCompleteEvent)
            }

            if (completeEvent != null) {
                this._armature!!._dragonBones!!.bufferEvent(completeEvent)
            }
        }
    }

    fun setCurrentTime(value: Float) {
        this._setCurrentTime(value)
        this._frameIndex = -1
    }
}
