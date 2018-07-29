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
		private var __hashCode: Int = 0
		private var _defaultMaxCount: Int = 3000
		private val _maxCountMap: LinkedHashMap<KClass<*>, Int> = LinkedHashMap()
		private val _poolsMap: LinkedHashMap<KClass<*>, ArrayList<BaseObject>> = LinkedHashMap()
		private val factories = LinkedHashMap<KClass<*>, () -> BaseObject>()

		private fun <T : BaseObject> _returnObject(obj: T) {
			val classType = obj::class
			val maxCount = BaseObject._maxCountMap[classType] ?: BaseObject._defaultMaxCount
			val pool = BaseObject.getPool(classType)
			if (pool.length < maxCount) {
				if (!obj._isInPool) {
					obj._isInPool = true
					(pool as ArrayList<BaseObject>).add(obj)
				} else {
					console.warn("The object is already in the pool.")
				}
			} else {
			}
		}

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
		fun <T : BaseObject> borrowObject(clazz: KClass<T>): T {
			val pool = getPool(clazz)
			val obj = if (pool.isNotEmpty()) pool.removeAt(pool.size - 1) else createInstance(clazz)
			obj._onClear()
			obj._isInPool = false
			return obj
		}

		inline fun <reified T : BaseObject> borrowObject(): T = borrowObject(T::class)

		@Suppress("UNCHECKED_CAST")
		private fun <T : BaseObject> createInstance(clazz: KClass<T>): T {
			val factory = factories[clazz] ?: TODO("Missing createInstance $clazz")
			return factory() as T
		}

		fun <T : BaseObject> register(clazz: KClass<T>, factory: () -> T) = run {
			factories[clazz] = factory
		}

		inline fun <reified T : BaseObject> register(noinline factory: () -> T) = register(T::class, factory)


		init {
			register { Animation() }
			register { EventObject() }
			register { DisplayFrame() }
			register { AnimationConfig() }
			register { BlendState() }
			register { IKConstraintTimelineState() }
			register { BoneAllTimelineState() }
			register { BoneTranslateTimelineState() }
			register { BoneRotateTimelineState() }
			register { BoneScaleTimelineState() }
			register { SlotDisplayTimelineState() }
			register { SlotZIndexTimelineState() }
			register { SlotColorTimelineState() }
			register { DeformTimelineState() }
			register { AlphaTimelineState() }
			register { ZOrderTimelineState() }
			register { SurfaceTimelineState() }
			register { AnimationProgressTimelineState() }
			register { AnimationWeightTimelineState() }
			register { AnimationParametersTimelineState() }
			register { ArmatureData() }
			register { CanvasData() }
			register { BoneData() }
			register { SurfaceData() }
			register { Surface() }
			register { Bone() }
			register { IKConstraint() }
			register { PathConstraint() }
			register { IKConstraintData() }
			register { PathConstraintData() }
			register { SlotData() }
			register { SkinData() }
			register { ImageDisplayData() }
			register { ArmatureDisplayData() }
			register { MeshDisplayData() }
			register { BoundingBoxDisplayData() }
			register { PathDisplayData() }
			register { RectangleBoundingBoxData() }
			register { EllipseBoundingBoxData() }
			register { PolygonBoundingBoxData() }
			register { AnimationData() }
			register { AnimationTimelineData() }
			register { TimelineData() }
			register { ActionData() }
			register { UserData() }
			register { WeightData() }
			register { DragonBonesData() }
			register { Armature() }
			register { AnimationState() }
			register { ActionTimelineState() }
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
	val _hashCode: Int = BaseObject.__hashCode++ // @TODO: Kotlin.JS hashCode produces a compiler error in JS
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
