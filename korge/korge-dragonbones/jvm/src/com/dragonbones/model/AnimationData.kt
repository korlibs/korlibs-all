package com.dragonbones.model

import com.dragonbones.core.BaseObject
import com.dragonbones.util.Array
import com.dragonbones.util.BoolArray
import com.dragonbones.util.IntArray

import java.util.HashMap

/**
 * 动画数据。
 *
 * @version DragonBones 3.0
 * @language zh_CN
 */
class AnimationData : BaseObject() {
    /**
     * @private
     */
    var frameIntOffset: Int = 0 // FrameIntArray.
    /**
     * @private
     */
    var frameFloatOffset: Int = 0 // FrameFloatArray.
    /**
     * @private
     */
    var frameOffset: Int = 0 // FrameArray.
    /**
     * 持续的帧数。 ([1~N])
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var frameCount: Int = 0
    /**
     * 播放次数。 [0: 无限循环播放, [1~N]: 循环播放 N 次]
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var playTimes: Int = 0
    /**
     * 持续时间。 (以秒为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var duration: Float = 0.toFloat()
    /**
     * @private
     */
    var scale: Float = 0.toFloat()
    /**
     * 淡入时间。 (以秒为单位)
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var fadeInTime: Float = 0.toFloat()
    /**
     * @private
     */
    var cacheFrameRate: Float = 0.toFloat()
    /**
     * 数据名称。
     *
     * @version DragonBones 3.0
     * @language zh_CN
     */
    var name: String
    /**
     * @private
     */
    var cachedFrames = BoolArray()
    /**
     * @private
     */
    val boneTimelines: MutableMap<String, Array<TimelineData>> = HashMap()
    /**
     * @private
     */
    val slotTimelines: MutableMap<String, Array<TimelineData>> = HashMap()
    /**
     * @private
     */
    val boneCachedFrameIndices: MutableMap<String, IntArray> = HashMap()
    /**
     * @private
     */
    val slotCachedFrameIndices: MutableMap<String, IntArray> = HashMap()
    /**
     * @private
     */
    var actionTimeline: TimelineData? = null // Initial value.
    /**
     * @private
     */
    var zOrderTimeline: TimelineData? = null // Initial value.
    /**
     * @private
     */
    var parent: ArmatureData? = null

    /**
     * @private
     */
    override fun _onClear() {
        for (k in this.boneTimelines.keys) {
            val timelineData = this.boneTimelines[k]
            for (kA in 0 until timelineData.size()) {
                timelineData.get(kA).returnToPool()
            }

            this.boneTimelines.remove(k)
        }

        for (k in this.slotTimelines.keys) {
            for (kA in 0 until this.slotTimelines.size) {
                this.slotTimelines[k].get(kA).returnToPool()
            }

            this.slotTimelines.remove(k)
        }

        this.boneCachedFrameIndices.clear()
        this.slotCachedFrameIndices.clear()

        if (this.actionTimeline != null) {
            this.actionTimeline!!.returnToPool()
        }

        if (this.zOrderTimeline != null) {
            this.zOrderTimeline!!.returnToPool()
        }

        this.frameIntOffset = 0
        this.frameFloatOffset = 0
        this.frameOffset = 0
        this.frameCount = 0
        this.playTimes = 0
        this.duration = 0f
        this.scale = 1f
        this.fadeInTime = 0f
        this.cacheFrameRate = 0f
        this.name = ""
        this.cachedFrames.clear()
        //this.boneTimelines.clear();
        //this.slotTimelines.clear();
        //this.boneCachedFrameIndices.clear();
        //this.slotCachedFrameIndices.clear();
        this.actionTimeline = null
        this.zOrderTimeline = null
        this.parent = null //
    }

    /**
     * @private
     */
    fun cacheFrames(frameRate: Float) {
        if (this.cacheFrameRate > 0f) { // TODO clear cache.
            return
        }

        this.cacheFrameRate = Math.max(Math.ceil((frameRate * this.scale).toDouble()), 1.0).toFloat()
        val cacheFrameCount =
            (Math.ceil((this.cacheFrameRate * this.duration).toDouble()) + 1).toInt() // Cache one more frame.

        this.cachedFrames.length = cacheFrameCount
        run {
            var i = 0
            val l = this.cachedFrames.size()
            while (i < l) {
                this.cachedFrames.setBool(i, false)
                ++i
            }
        }

        for (bone in this.parent!!.sortedBones) {
            val indices = IntArray(cacheFrameCount)
            var i = 0
            val l = indices.size()
            while (i < l) {
                indices.set(i, -1)
                ++i
            }

            this.boneCachedFrameIndices[bone.name] = indices
        }

        for (slot in this.parent!!.sortedSlots) {
            val indices = IntArray(cacheFrameCount)
            var i = 0
            val l = indices.size()
            while (i < l) {
                indices.set(i, -1)
                ++i
            }

            this.slotCachedFrameIndices[slot.name] = indices
        }
    }

    /**
     * @private
     */
    fun addBoneTimeline(bone: BoneData, timeline: TimelineData) {
        if (!this.boneTimelines.containsKey(bone.name)) {
            this.boneTimelines[bone.name] = Array()
        }
        val timelines = this.boneTimelines[bone.name]
        if (timelines.indexOf(timeline) < 0) {
            timelines.add(timeline)
        }
    }

    /**
     * @private
     */
    fun addSlotTimeline(slot: SlotData, timeline: TimelineData) {
        if (!this.slotTimelines.containsKey(slot.name)) {
            this.slotTimelines[slot.name] = Array()
        }
        val timelines = this.slotTimelines[slot.name]
        if (timelines.indexOf(timeline) < 0) {
            timelines.add(timeline)
        }
    }

    /**
     * @private
     */
    fun getBoneTimelines(name: String?): Array<TimelineData> {
        return this.boneTimelines[name]
    }

    /**
     * @private
     */
    fun getSlotTimeline(name: String?): Array<TimelineData> {
        return this.slotTimelines[name]
    }

    /**
     * @private
     */
    fun getBoneCachedFrameIndices(name: String?): IntArray {
        return this.boneCachedFrameIndices[name]
    }

    /**
     * @private
     */
    fun getSlotCachedFrameIndices(name: String?): IntArray {
        return this.slotCachedFrameIndices[name]
    }
}
