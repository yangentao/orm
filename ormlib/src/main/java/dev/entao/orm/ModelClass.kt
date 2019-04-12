@file:Suppress("unused")

package dev.entao.orm

import android.support.annotation.Keep
import dev.entao.appbase.sql.EQ
import dev.entao.appbase.sql.RowData
import dev.entao.appbase.sql.SQLQuery
import dev.entao.appbase.sql.Where
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * Created by entaoyang@163.com on 2017/4/5.
 */

@Keep
open class ModelClass<T : Model> {

	@Suppress("UNCHECKED_CAST")
	val modelClass: KClass<T> = javaClass.enclosingClass.kotlin as KClass<T>


	open fun delete(w: Where?): Int {
		return Pool.peek.deleteAll(modelClass, w)
	}

	open fun update(map: Map<KProperty<*>, Any?>, w: Where?): Int {
		return Pool.peek.updateProp(modelClass, map, w)
	}

	open fun update(p: Pair<KProperty<*>, Any?>, w: Where?): Int {
		return update(mapOf(p), w)
	}

	open fun update(p: Pair<KProperty<*>, Any?>, p2: Pair<KProperty<*>, Any?>, w: Where?): Int {
		return update(mapOf(p, p2), w)
	}

	open fun update(vararg ps: Pair<KProperty<*>, Any?>, block: () -> Where?): Int {
		return update(ps.toMap(), block())
	}

	open fun query(block: SQLQuery.() -> Unit): List<RowData> {
		return PoolPeek.findRows(block)
	}

	open fun count(w: Where?): Int {
		return PoolPeek.count(modelClass, w)
	}

	open fun findAll(block: SQLQuery.() -> Unit): List<T> {
		return PoolPeek.findAll<T>(modelClass, block)
	}

	open fun findAll(w: Where?): List<T> {
		return this.findAll(w) {}
	}

	open fun findAll(w: Where?, block: SQLQuery.() -> Unit): List<T> {
		return this.findAll {
			where(w)
			this.block()
		}
	}


	open fun findOne(w: Where?): T? {
		return PoolPeek.findOne(modelClass, w)
	}

	open fun findByKey(key: Any): T? {
		val pk = modelClass.modelPropPrimaryKey ?: return null
		return findOne(pk EQ key)
	}

	fun dump() {
		PoolPeek.dump(modelClass)
	}

}