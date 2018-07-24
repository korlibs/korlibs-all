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
	private fun _returnObject(object: BaseObject): Unit {
		val classType = String(object.constructor)
		val maxCount = classType in BaseObject._maxCountMap ? BaseObject._maxCountMap[classType] : BaseObject._defaultMaxCount
		val pool = BaseObject._poolsMap[classType] = BaseObject._poolsMap[classType] || []
		if (pool.length < maxCount) {
			if (!object._isInPool) {
				object._isInPool = true
				pool.push(object)
			}
			else {
				console.warn("The object is already in the pool.")
			}
		}
		else {
		}
	}

	public override fun toString(): String {
		throw new Error()
	}

	companion object {
		private var _hashCode: Int = 0
		private var _defaultMaxCount: Int = 3000
		private val _maxCountMap: Map<Int> = {}
		private val _poolsMap: Map<Array<BaseObject>> = {}

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
		public fun setMaxCount(objectConstructor: (() -> BaseObject)?, maxCount: Int): Unit {
			var maxCount = maxCount
			if (maxCount < 0 || maxCount != maxCount) { // isNaN
				maxCount = 0
			}

			if (objectConstructor != null) {
				val classType = String(objectConstructor)
				val pool = classType in BaseObject._poolsMap ? BaseObject._poolsMap[classType] : null
				if (pool !== null && pool.length > maxCount) {
					pool.length = maxCount
				}

				BaseObject._maxCountMap[classType] = maxCount
			}
			else {
				BaseObject._defaultMaxCount = maxCount

				for (classType in BaseObject._poolsMap) {
					val pool = BaseObject._poolsMap[classType]
					if (pool.length > maxCount) {
						pool.length = maxCount
					}

					if (classType in BaseObject._maxCountMap) {
						BaseObject._maxCountMap[classType] = maxCount
					}
				}
			}
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
		public fun clearPool(objectConstructor: (() -> BaseObject)? = null): Unit {
			if (objectConstructor !== null) {
				val classType = String(objectConstructor)
				val pool = if (classType in BaseObject._poolsMap) BaseObject._poolsMap[classType] else null
				if (pool != null && pool.size > 0) {
					pool.length = 0
				}
			}
			else {
				for (k in BaseObject._poolsMap) {
					val pool = BaseObject._poolsMap[k]
					pool.length = 0
				}
			}
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
		public fun <T  :  BaseObject> borrowObject(objectConstructor: { new(): T; }): T {
			val classType = String(objectConstructor)
			val pool = classType in BaseObject._poolsMap ? BaseObject._poolsMap[classType] : null
			if (pool !== null && pool.length > 0) {
				val object = pool.pop() as T
				object._isInPool = false
				return object
			}

			val obj = objectConstructor()
			obj._onClear()
			return obj
		}

		inline public fun <reified T  :  BaseObject> borrowObject(): T = TODO()
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
	public val hashCode: Int = BaseObject._hashCode++
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
	public fun returnToPool(): Unit {
		this._onClear()
		BaseObject._returnObject(this)
	}
}
