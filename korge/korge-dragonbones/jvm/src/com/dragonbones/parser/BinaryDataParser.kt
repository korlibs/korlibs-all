package com.dragonbones.parser

import com.dragonbones.core.BaseObject
import com.dragonbones.core.BinaryOffset
import com.dragonbones.core.TimelineType
import com.dragonbones.factory.BaseFactory
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.dragonbones.util.buffer.*
import com.dragonbones.util.json.JSON

import java.nio.charset.StandardCharsets

import com.dragonbones.util.Dynamic.*

/**
 * @private
 */
class BinaryDataParser : ObjectDataParser() {
    private var _binary: ArrayBuffer? = null
    private var _binaryOffset: Int = 0
    private var _intArray: ShortArray? = null
    private var _floatArray: FloatArray? = null
    private var _frameIntArray: ShortArray? = null
    private var _frameFloatArray: FloatArray? = null
    private var _frameArray: ShortArray? = null
    private var _timelineArray: CharArray? = null

    /*
    private boolean _inRange(float a, float min, float max) {
        return min <= a && a <= max;
    }

    private String _decodeUTF8(Uint8Array data) {
        int EOF_byte = -1;
        int EOF_code_point = -1;
        int FATAL_POINT = 0xFFFD;

        int pos = 0;
        String result = "";
        Integer code_point = null;
        int utf8_code_point = 0;
        int utf8_bytes_needed = 0;
        int utf8_bytes_seen = 0;
        int utf8_lower_boundary = 0;

        while (data.length() > pos) {

            int _byte = data.get(pos++);

            if (_byte == EOF_byte) {
                if (utf8_bytes_needed != 0) {
                    code_point = FATAL_POINT;
                } else {
                    code_point = EOF_code_point;
                }
            } else {
                if (utf8_bytes_needed == 0) {
                    if (this._inRange(_byte, 0x00, 0x7F)) {
                        code_point = _byte;
                    } else {
                        if (this._inRange(_byte, 0xC2, 0xDF)) {
                            utf8_bytes_needed = 1;
                            utf8_lower_boundary = 0x80;
                            utf8_code_point = _byte - 0xC0;
                        } else if (this._inRange(_byte, 0xE0, 0xEF)) {
                            utf8_bytes_needed = 2;
                            utf8_lower_boundary = 0x800;
                            utf8_code_point = _byte - 0xE0;
                        } else if (this._inRange(_byte, 0xF0, 0xF4)) {
                            utf8_bytes_needed = 3;
                            utf8_lower_boundary = 0x10000;
                            utf8_code_point = _byte - 0xF0;
                        } else {

                        }
                        utf8_code_point = utf8_code_point * (int) Math.pow(64, utf8_bytes_needed);
                        code_point = null;
                    }
                } else if (!this._inRange(_byte, 0x80, 0xBF)) {
                    utf8_code_point = 0;
                    utf8_bytes_needed = 0;
                    utf8_bytes_seen = 0;
                    utf8_lower_boundary = 0;
                    pos--;
                    code_point = _byte;
                } else {

                    utf8_bytes_seen += 1;
                    utf8_code_point = utf8_code_point + (_byte - 0x80) * (int) Math.pow(64, utf8_bytes_needed - utf8_bytes_seen);

                    if (utf8_bytes_seen != utf8_bytes_needed) {
                        code_point = null;
                    } else {

                        int cp = utf8_code_point;
                        int lower_boundary = utf8_lower_boundary;
                        utf8_code_point = 0;
                        utf8_bytes_needed = 0;
                        utf8_bytes_seen = 0;
                        utf8_lower_boundary = 0;
                        if (this._inRange(cp, lower_boundary, 0x10FFFF) && !this._inRange(cp, 0xD800, 0xDFFF)) {
                            code_point = cp;
                        } else {
                            code_point = _byte;
                        }
                    }

                }
            }
            //Decode string
            if (code_point != null && code_point != EOF_code_point) {
                if (code_point <= 0xFFFF) {
                    if (code_point > 0) result += StringUtil.fromCodePoint(code_point);
                } else {
                    code_point -= 0x10000;
                    result += StringUtil.fromCharCode(0xD800 + ((code_point >> 10) & 0x3ff));
                    result += StringUtil.fromCharCode(0xDC00 + (code_point & 0x3ff));
                }
            }
        }

        return result;
    }

    private String _getUTF16Key(String value) {
        for (int i = 0, l = value.length(); i < l; ++i) {
            if (value.charAt(i) > 255) {
                return encodeURI(value);
            }
        }

        return value;
    }

    private String encodeURI(String value) {
        throw new RuntimeException("encodeURI not implemented");
    }
    */

    private fun _parseBinaryTimeline(type: TimelineType, offset: Int, timelineData: TimelineData?): TimelineData {
        val timeline = timelineData ?: BaseObject.borrowObject(TimelineData::class.java)
        timeline.type = type
        timeline.offset = offset

        this._timeline = timeline

        val keyFrameCount = this._timelineArray!![timeline.offset + BinaryOffset.TimelineKeyFrameCount.v]
        if (keyFrameCount == 1) {
            timeline.frameIndicesOffset = -1
        } else {
            val totalFrameCount = this._animation!!.frameCount + 1 // One more frame than animation.
            val frameIndices = this._data!!.frameIndices
            timeline.frameIndicesOffset = frameIndices.length()
            frameIndices.incrementLength(totalFrameCount)

            var i = 0
            var iK = 0
            var frameStart = 0
            var frameCount = 0
            while (i < totalFrameCount) {
                if (frameStart + frameCount <= i && iK < keyFrameCount) {
                    frameStart =
                            this._frameArray!![this._animation!!.frameOffset + this._timelineArray!![timeline.offset + BinaryOffset.TimelineFrameOffset.v + iK]]
                    if (iK == keyFrameCount - 1) {
                        frameCount = this._animation!!.frameCount - frameStart
                    } else {
                        frameCount = this._frameArray!![this._animation!!.frameOffset + this._timelineArray!![timeline.offset + BinaryOffset.TimelineFrameOffset.v + iK + 1]] -
                                frameStart
                    }

                    iK++
                }

                frameIndices.set(timeline.frameIndicesOffset + i, iK - 1)
                ++i
            }
        }

        this._timeline = null //

        return timeline
    }

    /**
     * @private
     */
    override fun _parseMesh(rawData: Any, mesh: MeshDisplayData) {
        mesh.offset = getInt(rawData, ObjectDataParser.OFFSET)

        val weightOffset = this._intArray!![mesh.offset + BinaryOffset.MeshWeightOffset.v]
        if (weightOffset >= 0) {
            val weight = BaseObject.borrowObject(WeightData::class.java)
            val vertexCount = this._intArray!![mesh.offset + BinaryOffset.MeshVertexCount.v]
            val boneCount = this._intArray!![weightOffset + BinaryOffset.WeigthBoneCount.v]
            weight.offset = weightOffset
            weight.bones.setLength(boneCount)

            for (i in 0 until boneCount) {
                val boneIndex = this._intArray!![weightOffset + BinaryOffset.WeigthBoneIndices.v + i]
                weight.bones.set(i, this._rawBones.get(boneIndex))
            }

            var boneIndicesOffset = weightOffset + BinaryOffset.WeigthBoneIndices.v + boneCount
            var i = 0
            while (i < vertexCount) {
                val vertexBoneCount = this._intArray!![boneIndicesOffset++]
                weight.count += vertexBoneCount
                boneIndicesOffset += vertexBoneCount
                ++i
            }

            mesh.weight = weight
        }
    }

    /**
     * @private
     */
    override fun _parsePolygonBoundingBox(rawData: Any): PolygonBoundingBoxData {
        val polygonBoundingBox = BaseObject.borrowObject(PolygonBoundingBoxData::class.java)
        polygonBoundingBox.offset = getInt(rawData, ObjectDataParser.OFFSET)
        polygonBoundingBox.vertices = this._floatArray

        return polygonBoundingBox
    }

    /**
     * @private
     */
    override fun _parseAnimation(rawData: Any): AnimationData {
        val animation = BaseObject.borrowObject(AnimationData::class.java)
        animation.frameCount = Math.max(getInt(rawData, ObjectDataParser.DURATION, 1), 1)
        animation.playTimes = getInt(rawData, ObjectDataParser.PLAY_TIMES, 1)
        animation.duration = animation.frameCount / this._armature!!.frameRate
        animation.fadeInTime = getFloat(rawData, ObjectDataParser.FADE_IN_TIME, 0f)
        animation.scale = getFloat(rawData, ObjectDataParser.SCALE, 1f)
        animation.name = getString(rawData, ObjectDataParser.NAME, ObjectDataParser.DEFAULT_NAME)
        if (animation.name.length == 0) {
            animation.name = ObjectDataParser.DEFAULT_NAME
        }

        // Offsets.
        val offsets = getIntArray(rawData, ObjectDataParser.OFFSET)
        animation.frameIntOffset = offsets!!.get(0)
        animation.frameFloatOffset = offsets.get(1)
        animation.frameOffset = offsets.get(2)

        this._animation = animation

        if (`in`(rawData, ObjectDataParser.ACTION)) {
            animation.actionTimeline =
                    this._parseBinaryTimeline(TimelineType.Action, getInt(rawData, ObjectDataParser.ACTION), null)
        }

        if (`in`(rawData, ObjectDataParser.Z_ORDER)) {
            animation.zOrderTimeline =
                    this._parseBinaryTimeline(TimelineType.ZOrder, getInt(rawData, ObjectDataParser.Z_ORDER), null)
        }

        if (`in`(rawData, ObjectDataParser.BONE)) {
            val rawTimeliness = getArray<Any>(rawData, ObjectDataParser.BONE)
            for (k in 0 until rawTimeliness!!.length()) {
                val rawTimelines = rawTimeliness.getObject(k) as ArrayBase<Any>
                val bone = this._armature!!.getBone("" + k) ?: continue

                var i = 0
                val l = rawTimelines.size()
                while (i < l) {
                    val timelineType = rawTimelines.getObject(i) as Int
                    val timelineOffset = rawTimelines.getObject(i + 1) as Int
                    val timeline = this._parseBinaryTimeline(TimelineType.values[timelineType], timelineOffset, null)
                    this._animation!!.addBoneTimeline(bone, timeline)
                    i += 2
                }
            }
        }

        if (`in`(rawData, ObjectDataParser.SLOT)) {
            val rawTimeliness = getArray<Any>(rawData, ObjectDataParser.SLOT)
            for (k in 0 until rawTimeliness!!.size()) {
                val rawTimelines = rawTimeliness.getObject(k) as ArrayBase<*>

                val slot = this._armature!!.getSlot("" + k) ?: continue

                var i = 0
                val l = rawTimelines.size()
                while (i < l) {
                    val timelineType = rawTimelines.getObject(i) as Int
                    val timelineOffset = rawTimelines.getObject(i + 1) as Int
                    val timeline = this._parseBinaryTimeline(TimelineType.values[timelineType], timelineOffset, null)
                    this._animation!!.addSlotTimeline(slot, timeline)
                    i += 2
                }
            }
        }

        this._animation = null

        return animation
    }

    /**
     * @private
     */
    override fun _parseArray(rawData: Any) {
        val offsets = getIntArray(rawData, ObjectDataParser.OFFSET)

        this._intArray = Int16Array(
            this._binary,
            this._binaryOffset + offsets!!.get(0),
            offsets.get(1) / Int16Array.BYTES_PER_ELEMENT
        )
        this._data!!.intArray = this._intArray
        this._floatArray = Float32Array(
            this._binary,
            this._binaryOffset + offsets.get(2),
            offsets.get(3) / Float32Array.BYTES_PER_ELEMENT
        )
        this._data!!.floatArray = this._floatArray
        this._frameIntArray = Int16Array(
            this._binary,
            this._binaryOffset + offsets.get(4),
            offsets.get(5) / Int16Array.BYTES_PER_ELEMENT
        )
        this._data!!.frameIntArray = this._frameIntArray
        this._frameFloatArray = Float32Array(
            this._binary,
            this._binaryOffset + offsets.get(6),
            offsets.get(7) / Float32Array.BYTES_PER_ELEMENT
        )
        this._data!!.frameFloatArray = this._frameFloatArray
        this._frameArray = Int16Array(
            this._binary,
            this._binaryOffset + offsets.get(8),
            offsets.get(9) / Int16Array.BYTES_PER_ELEMENT
        )
        this._data!!.frameArray = this._frameArray
        this._timelineArray = Uint16Array(
            this._binary,
            this._binaryOffset + offsets.get(10),
            offsets.get(11) / Uint16Array.BYTES_PER_ELEMENT
        )
        this._data!!.timelineArray = this._timelineArray
    }

    override fun parseDragonBonesDataInstance(rawData: Any): DragonBonesData? {
        return parseDragonBonesData(rawData, 1f)
    }

    /**
     * @inheritDoc
     */
    override fun parseDragonBonesData(rawData: Any, scale: Float): DragonBonesData? {
        Console._assert(rawData != null && rawData is ArrayBuffer)
        val buffer = rawData as ArrayBuffer

        val tag = Uint8Array(buffer, 0, 8)
        if (tag.get(0) != 'D'.toInt() || tag.get(1) != 'B'.toInt() || tag.get(2) != 'D'.toInt() || tag.get(3) != 'T'.toInt()) {
            Console._assert(false, "Nonsupport data.")
            return null
        }

        val headerLength = Uint32Array(buffer, 8, 1).get(0)

        //String headerString = this._decodeUTF8(new Uint8Array(buffer, 8 + 4, headerLength));
        val headerString = String(buffer.getBytes(8 + 4, headerLength), StandardCharsets.UTF_8)

        val header = JSON.parse(headerString)

        this._binary = buffer
        this._binaryOffset = 8 + 4 + headerLength

        return super.parseDragonBonesData(header!!, scale)
    }

    companion object {

        /**
         * @private
         */
        private var _binaryDataParserInstance: BinaryDataParser? = null

        /**
         * @see BaseFactory.parseDragonBonesData
         */
        val instance: BinaryDataParser
            @Deprecated("已废弃，请参考 @see")
            get() {
                if (BinaryDataParser._binaryDataParserInstance == null) {
                    BinaryDataParser._binaryDataParserInstance = BinaryDataParser()
                }

                return BinaryDataParser._binaryDataParserInstance
            }
    }
}
