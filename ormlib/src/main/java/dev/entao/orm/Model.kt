@file:Suppress("unused")

package dev.entao.orm

import android.content.ContentValues
import android.support.annotation.Keep
import dev.entao.appbase.sql.Where
import dev.entao.base.getValue
import dev.entao.base.nameProp
import dev.entao.json.Yson
import dev.entao.json.YsonObject
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

/**
 * Created by entaoyang@163.com on 2017/3/31.
 */

@Keep
open class Model(val model: YsonObject) {

	fun hasKey(p: KProperty<*>): Boolean {
		return hasKey(p.nameProp)
	}

	fun hasKey(key: String): Boolean {
		return model.containsKey(key)
	}

	fun removeProperty(p: KProperty<*>) {
		model.removeProperty(p)
	}


	fun saveByKey(vararg ps: KMutableProperty<*>): Boolean {
		val pk = this::class.modelPropPrimaryKey ?: return false
		return if (hasKey(pk)) {
			updateByKey(*ps)
		} else {
			insert()
		}
	}

	fun insert(): Boolean {
		return -1L != Pool.peek.insert(this)
	}

	fun update(vararg ps: KMutableProperty<*>, block: () -> Where?): Int {
		val w = block()
		val psList = if (ps.isEmpty()) {
			this::class.modelPropList
		} else {
			ps.toList()
		}

		val map = HashMap<String, Any?>(32)
		psList.forEach {
			map[it.nameProp] = it.getValue(this)
		}
		return Pool.peek.update(this::class, map, w)

	}
	fun updateByKey(block: () -> Unit): Boolean {
		val ls = this.model.gather(block)
		if (ls.isNotEmpty()) {
			return this.updateByKey(ls)
		}
		return false
	}
	fun updateByKey(ps: List<KMutableProperty<*>>): Boolean {
		return Pool.peek.updateByKey(this, ps)

	}

	fun updateByKey(vararg ps: KMutableProperty<*>): Boolean {
		return Pool.peek.updateByKey(this, ps.toList())

	}


	fun fromYsonObject(yo: YsonObject) {
		val st = this::class.modelPropKeySet
		yo.forEach {
			val k = it.key
			if (k in st) {
				model[k] = it.value
			}
		}
	}

	fun toJson(vararg ps: KProperty<*>): YsonObject {
		val jo = YsonObject()
		val st = this::class.modelPropKeySet

		val ls = if (ps.isEmpty()) {
			this::class.modelPropList
		} else {
			ps.toList()
		}

		for (p in ls) {
			val k = p.nameProp
			if (k in st) {
				val v = p.getValue(this)
				jo.any(k, v)
			}
		}

		return jo
	}

	fun fillJson(jo: YsonObject, vararg ps: KProperty<*>): YsonObject {
		val ls = if (ps.isEmpty()) {
			ps.toList()
		} else {
			this::class.modelPropList
		}
		val st = this::class.modelPropKeySet
		for (p in ls) {
			val k = p.nameProp
			if (k in st) {
				val v = p.getValue(this)
				jo.any(k, v)
			}
		}

		return jo
	}

	override fun toString(): String {
		return Yson.toYson(model).toString()
	}

	fun toContentValues(): ContentValues {
		val ks = this::class.modelPropKeySet
		val m2 = model.filterKeys { it in ks }
		return dev.entao.appbase.sql.mapToContentValues(m2)
	}


}