package com.dragonbones.core

import com.dragonbones.util.Array
import com.dragonbones.util.Console

import java.util.HashMap

/**
 * 基础对象。
 *
 * @version DragonBones 4.5
 * @language zh_CN
 */
abstract class BaseObject {

    /**
     * 对象的唯一标识。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    val hashCode = BaseObject._hashCode++
    internal var _isInPool = false

    /**
     * @private
     */
    protected abstract fun _onClear()

    /**
     * 清除数据并返还对象池。
     *
     * @version DragonBones 4.5
     * @language zh_CN
     */
    fun returnToPool() {
        this._onClear()
        BaseObject._returnObject(this)
    }

    companion object {
        private var _hashCode = 0
        private var _defaultMaxCount = 1000
        private val _maxCountMap = HashMap<Class<out BaseObject>, Int>()
        private val _poolsMap = HashMap<Class<out BaseObject>, Array<BaseObject>>()

        private fun _returnObject(`object`: BaseObject) {
            val classType = `object`.javaClass
            val maxCount =
                if (BaseObject._maxCountMap.containsKey(classType)) BaseObject._defaultMaxCount else BaseObject._maxCountMap[classType]
            if (!BaseObject._poolsMap.containsKey(classType)) {
                BaseObject._poolsMap[classType] = Array()
            }
            val pool = BaseObject._poolsMap[classType]!!
            if (pool.size() < maxCount!!) {
                if (!`object`._isInPool) {
                    `object`._isInPool = true
                    pool.add(`object`)
                } else {
                    Console._assert(false, "The object is already in the pool.")
                }
            } else {
            }
        }

        /**
         * 设置每种对象池的最大缓存数量。
         *
         * @param classType 对象类。
         * @param maxCount  最大缓存数量。 (设置为 0 则不缓存)
         * @version DragonBones 4.5
         * @language zh_CN
         */
        fun setMaxCount(classType: Class<BaseObject>?, maxCount: Int) {
            var maxCount = maxCount
            if (maxCount < 0) { // isNaN
                maxCount = 0
            }

            if (classType != null) {
                val pool = BaseObject._poolsMap[classType]
                if (pool != null && pool.size() > maxCount) {
                    pool.length = maxCount
                }

                BaseObject._maxCountMap[classType] = maxCount
            } else {
                BaseObject._defaultMaxCount = maxCount
                for (classType2 in BaseObject._poolsMap.keys) {
                    if (BaseObject._maxCountMap.containsKey(classType2)) {
                        continue
                    }

                    val pool = BaseObject._poolsMap[classType2]
                    if (pool.size() > maxCount) {
                        pool.setLength(maxCount)
                    }

                    BaseObject._maxCountMap[classType2] = maxCount
                }
            }
        }

        fun clearPool() {
            for (pool in BaseObject._poolsMap.values) {
                pool.clear()
            }
        }

        /**
         * 清除对象池缓存的对象。
         *
         * @param classType 对象类。 (不设置则清除所有缓存)
         * @version DragonBones 4.5
         * @language zh_CN
         */
        fun clearPool(classType: Class<BaseObject>) {
            val pool = BaseObject._poolsMap[classType]
            if (pool != null && pool.size() > 0) {
                pool.clear()
            }
        }

        /**
         * 从对象池中创建指定对象。
         *
         * @param classType 对象类。
         * @version DragonBones 4.5
         * @language zh_CN
         */

        fun <T : BaseObject> borrowObject(classType: Class<T>): T {
            val pool = BaseObject._poolsMap[classType]
            if (pool != null && pool.size() > 0) {
                val `object` = pool.popObject() as T
                `object`._isInPool = false
                return `object`
            }

            try {
                val `object` = classType.newInstance()
                `object`._onClear()
                return `object`
            } catch (e: InstantiationException) {
                e.printStackTrace()
                throw RuntimeException(e)
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                throw RuntimeException(e)
            }

        }
    }
}
