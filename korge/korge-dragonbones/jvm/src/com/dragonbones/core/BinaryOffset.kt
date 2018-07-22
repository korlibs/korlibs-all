package com.dragonbones.core

/**
 * @private
 */
enum class BinaryOffset private constructor(val v: Int) {
    WeigthBoneCount(0),
    WeigthFloatOffset(1),
    WeigthBoneIndices(2),

    MeshVertexCount(0),
    MeshTriangleCount(1),
    MeshFloatOffset(2),
    MeshWeightOffset(3),
    MeshVertexIndices(4),

    TimelineScale(0),
    TimelineOffset(1),
    TimelineKeyFrameCount(2),
    TimelineFrameValueCount(3),
    TimelineFrameValueOffset(4),
    TimelineFrameOffset(5),

    FramePosition(0),
    FrameTweenType(1),
    FrameTweenEasingOrCurveSampleCount(2),
    FrameCurveSamples(3),

    FFDTimelineMeshOffset(0),
    FFDTimelineFFDCount(1),
    FFDTimelineValueCount(2),
    FFDTimelineValueOffset(3),
    FFDTimelineFloatOffset(4)
}
