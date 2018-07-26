/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2012-2018 DragonBones team and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.dragonbones.core

import com.dragonbones.animation.*
import com.dragonbones.armature.*
import com.dragonbones.event.*
import com.dragonbones.model.*
import com.dragonbones.util.*
import com.soywiz.korma.geom.*
import kotlin.reflect.*

/**
 * - The BaseObject is the base class for all objects in the DragonBones framework.
 * All BaseObject instances are cached to the object pool to reduce the performance consumption of frequent requests for memory or memory recovery.
 * @version DragonBones 4.5
 * @language en_US
 */
/**
 * - 基础对象，通常 DragonBones 的对象都继承自该类。
 * 所有基础对象的实例都会缓存到对象池，以减少频繁申请内存或内存回收的性能消耗。
 * @version DragonBones 4.5
 * @language zh_CN
 */
abstract class BaseObject {
	override fun toString(): String = "BaseObject.Unknown"

	companion object {
		private fun <T : BaseObject> _returnObject(obj: T) {
			val classType = obj::class
			val maxCount = BaseObject._maxCountMap[classType] ?: BaseObject._defaultMaxCount
			val pool = BaseObject.getPool(classType)
			if (pool.length < maxCount) {
				if (!obj._isInPool) {
					obj._isInPool = true
					(pool as ArrayList<BaseObject>).add(obj)
				}
				else {
					console.warn("The object is already in the pool.")
				}
			}
			else {
			}
		}

		private var _hashCode: Int = 0
		private var _defaultMaxCount: Int = 3000
		private val _maxCountMap: LinkedHashMap<KClass<*>, Int> = LinkedHashMap()
		private val _poolsMap: LinkedHashMap<KClass<*>, ArrayList<BaseObject>> = LinkedHashMap()

		internal fun <T : BaseObject> getPool(clazz: KClass<T>): ArrayList<T> {
			return _poolsMap.getOrPut(clazz) { arrayListOf() } as ArrayList<T>
		}

		/**
		 * - Set the maximum cache count of the specify object pool.
		 * @param objectConstructor - The specify class. (Set all object pools max cache count if not set)
		 * @param maxCount - Max count.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 设置特定对象池的最大缓存数量。
		 * @param objectConstructor - 特定的类。 (不设置则设置所有对象池的最大缓存数量)
		 * @param maxCount - 最大缓存数量。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		fun setMaxCount(clazz: KClass<out BaseObject>, maxCount: Int) {
			_maxCountMap[clazz] = maxCount
			val pool = getPool(clazz)
			pool.lengthSet = maxCount

			//var maxCount = maxCount
			//if (maxCount < 0 || maxCount != maxCount) { // isNaN
			//	maxCount = 0
			//}
//
			//if (objectConstructor != null) {
			//	val classType = String(objectConstructor)
			//	val pool = if (classType in BaseObject._poolsMap) BaseObject._poolsMap[classType] else null
			//	if (pool != null && pool.length > maxCount) {
			//		pool.length = maxCount
			//	}
//
			//	BaseObject._maxCountMap[classType] = maxCount
			//}
			//else {
			//	BaseObject._defaultMaxCount = maxCount
//
			//	for (classType in BaseObject._poolsMap) {
			//		val pool = BaseObject._poolsMap[classType]!!
			//		if (pool.length > maxCount) {
			//			pool.length = maxCount
			//		}
//
			//		if (classType in BaseObject._maxCountMap) {
			//			BaseObject._maxCountMap[classType] = maxCount
			//		}
			//	}
			//}
		}
		/**
		 * - Clear the cached instances of a specify object pool.
		 * @param objectConstructor - Specify class. (Clear all cached instances if not set)
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 清除特定对象池的缓存实例。
		 * @param objectConstructor - 特定的类。 (不设置则清除所有缓存的实例)
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		fun <T : BaseObject> clearPool(clazz: KClass<T>): Unit {
			getPool(clazz).clear()
			//if (objectConstructor != null) {
			//	val classType = String(objectConstructor)
			//	val pool = if (classType in BaseObject._poolsMap) BaseObject._poolsMap[classType] else null
			//	if (pool != null && pool.size > 0) {
			//		pool.lengthSet = 0
			//	}
			//}
			//else {
			//	for (k in BaseObject._poolsMap) {
			//		val pool = BaseObject._poolsMap[k]
			//		pool.length = 0
			//	}
			//}
		}
		/**
		 * - Get an instance of the specify class from object pool.
		 * @param objectConstructor - The specify class.
		 * @version DragonBones 4.5
		 * @language en_US
		 */
		/**
		 * - 从对象池中获取特定类的实例。
		 * @param objectConstructor - 特定的类。
		 * @version DragonBones 4.5
		 * @language zh_CN
		 */
		fun <T  :  BaseObject> borrowObject(clazz: KClass<T>): T {
			val pool = getPool(clazz)
			val obj = if (pool.isNotEmpty()) pool.removeAt(pool.size - 1) else createInstance(clazz)
			obj._onClear()
			return obj

			//val classType = String(objectConstructor)
			//val pool = classType in BaseObject._poolsMap ? BaseObject._poolsMap[classType] : null
			//if (pool != null && pool.length > 0) {
			//	val object = pool.pop() as T
			//	object._isInPool = false
			//	return object
			//}
			//
			//val obj = objectConstructor()
			//obj._onClear()
			//return obj
		}

		inline fun <reified T  :  BaseObject> borrowObject(): T = borrowObject(T::class)

		@Suppress("UNCHECKED_CAST")
		private fun <T  :  BaseObject> createInstance(clazz: KClass<T>): T {
			return when (clazz) {
				Animation::class -> Animation() as T
				EventObject::class -> EventObject() as T
				DisplayFrame::class -> DisplayFrame() as T
				AnimationConfig::class -> AnimationConfig() as T
				BlendState::class -> BlendState() as T
				IKConstraintTimelineState::class -> IKConstraintTimelineState() as T
				BoneAllTimelineState::class -> BoneAllTimelineState() as T
				BoneTranslateTimelineState::class -> BoneTranslateTimelineState() as T
				BoneRotateTimelineState::class -> BoneRotateTimelineState() as T
				BoneScaleTimelineState::class -> BoneScaleTimelineState() as T
				SlotDisplayTimelineState::class -> SlotDisplayTimelineState() as T
				SlotZIndexTimelineState::class -> SlotZIndexTimelineState() as T
				SlotColorTimelineState::class -> SlotColorTimelineState() as T
				DeformTimelineState::class -> DeformTimelineState() as T
				AlphaTimelineState::class -> AlphaTimelineState() as T
				ZOrderTimelineState::class -> ZOrderTimelineState() as T
				SurfaceTimelineState::class -> SurfaceTimelineState() as T
				AnimationProgressTimelineState::class -> AnimationProgressTimelineState() as T
				AnimationWeightTimelineState::class -> AnimationWeightTimelineState() as T
				AnimationParametersTimelineState::class -> AnimationParametersTimelineState() as T
				ArmatureData::class -> ArmatureData() as T
				CanvasData::class -> CanvasData() as T
				BoneData::class -> BoneData() as T
				SurfaceData::class -> SurfaceData() as T
				Surface::class -> Surface() as T
				Bone::class -> Bone() as T
				IKConstraint::class -> IKConstraint() as T
				PathConstraint::class -> PathConstraint() as T
				IKConstraintData::class -> IKConstraintData() as T
				PathConstraintData::class -> PathConstraintData() as T
				SlotData::class -> SlotData() as T
				SkinData::class -> SkinData() as T
				ImageDisplayData::class -> ImageDisplayData() as T
				ArmatureDisplayData::class -> ArmatureDisplayData() as T
				MeshDisplayData::class -> MeshDisplayData() as T
				BoundingBoxDisplayData::class -> BoundingBoxDisplayData() as T
				PathDisplayData::class -> PathDisplayData() as T
				RectangleBoundingBoxData::class -> RectangleBoundingBoxData() as T
				EllipseBoundingBoxData::class -> EllipseBoundingBoxData() as T
				PolygonBoundingBoxData::class -> PolygonBoundingBoxData() as T
				AnimationData::class -> AnimationData() as T
				AnimationTimelineData::class -> AnimationTimelineData() as T
				TimelineData::class -> TimelineData() as T
				ActionData::class -> ActionData() as T
				UserData::class -> UserData() as T
				WeightData::class -> WeightData() as T
				DragonBonesData::class -> DragonBonesData() as T
				else -> TODO("Missing createInstance $clazz")
			}
		}
	}


	/**
	 * - A unique identification number assigned to the object.
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 分配给此实例的唯一标识号。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	val hashCode: Int = BaseObject._hashCode++
	private var _isInPool: Boolean = false

	protected abstract fun _onClear(): Unit
	/**
	 * - Clear the object and return it back to object pool。
	 * @version DragonBones 4.5
	 * @language en_US
	 */
	/**
	 * - 清除该实例的所有数据并将其返还对象池。
	 * @version DragonBones 4.5
	 * @language zh_CN
	 */
	fun returnToPool(): Unit {
		this._onClear()
		BaseObject._returnObject(this)
	}
}
