@file:Suppress("unused")

package dev.entao.orm

import android.annotation.SuppressLint
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dev.entao.appbase.sql.*
import dev.entao.base.*
import dev.entao.json.YsonNum
import dev.entao.json.YsonObject
import dev.entao.json.createYsonModel
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty


class Conn(val db: SQLiteDatabase) : Closeable {


	override fun close() {
		try {
			db.close()
		} catch (t: Throwable) {
			t.printStackTrace()
		}
	}

	fun <R> transaction(block: Conn.() -> R): R {
		var ok = true
		try {
			db.beginTransaction()
			return this.block()
		} catch (ex: Throwable) {
			ok = false
			ex.printStackTrace()
			throw ex
		} finally {
			if (ok) {
				db.setTransactionSuccessful()
			}
			db.endTransaction()
		}
	}

	fun deleteAll(cls: KClass<*>, w: Where?): Int {
		TableCreatorX.check(db, cls)
		return db.delete(cls.nameClass, w?.value, w?.sqlArgs)
	}

	fun delete(cls: KClass<*>, w: Where): Int {
		return this.deleteAll(cls, w)
	}

	fun deleteByKey(model: Model): Int {
		val pk = model::class.modelPropPrimaryKey ?: return 0
		val v = pk.getValue(model) ?: return 0
		return this.delete(model::class, pk EQ v)
	}

	//返回-1失败
	fun insert(model: Model): Long {
		return insertOrreplace(model, false)
	}

	//返回-1失败
	fun replace(model: Model): Long {
		return insertOrreplace(model, true)
	}

	//返回-1失败
	fun insertOrreplace(model: Model, isReplace: Boolean): Long {
		TableCreatorX.check(db, model::class)
		val cv = model.toContentValues()
		val pk = model::class.modelPropPrimaryKey
		var assignId = false
		if (pk != null) {
			if (pk.hasAnnotation<AutoInc>()) {
				if (!model.model.containsKey(pk.nameProp)) {
					assignId = true
				} else if ((model.model[pk.nameProp] as? YsonNum)?.data?.toInt() == 0) {
					cv.remove(pk.nameProp)
					assignId = true
				}
			}
		}
		val ret = if (isReplace) {
			db.replace(model::class.nameClass, null, cv)
		} else {
			db.insert(model::class.nameClass, null, cv)
		}
		if (ret != -1L && pk != null) {
			if (assignId) {
				if (pk.isTypeInt) {
					pk.setValue(model, ret.toInt())
				} else if (pk.isTypeLong) {
					pk.setValue(model, ret)
				}
			}
		}
		return ret
	}


	fun updateProp(cls: KClass<*>, map: Map<KProperty<*>, Any?>, w: Where?): Int {
		val m2 = map.mapKeys { it.key.nameProp }
		return this.update(cls, m2, w)
	}

	fun update(cls: KClass<*>, map: Map<String, Any?>, w: Where?): Int {
		TableCreatorX.check(db, cls)
		val st = cls.modelPropKeySet
		val m = map.filter { it.key in st }
		val cv = mapToContentValues(m)
		return db.update(cls.nameClass, cv, w?.value, w?.sqlArgs)
	}

	fun updateByKey(model: Model): Boolean {
		return this.updateByKey(model, emptyList())
	}

	fun updateByKey(model: Model, ps: List<KMutableProperty<*>>): Boolean {
		val pk = model::class.modelPropPrimaryKey ?: return false
		val pv = pk.getValue(model) ?: return false

		val m = if (ps.isNotEmpty()) {
			val ks = ps.map { it.nameProp }.toSet()
			model.model.filter { it.key in ks }
		} else {
			model.model.filter { it.key != pk.nameProp }
		}
		return this.update(model::class, m, pk EQ pv) > 0
	}

	fun save(model: Model): Boolean {
		val pk = model::class.modelPropPrimaryKey ?: return false
		val hasPk = model.model.containsKey(pk.nameProp)
		return if (hasPk) {
			updateByKey(model)
		} else {
			this.insert(model) != -1L
		}
	}


	fun count(cls: KClass<*>, w: Where?): Int {
		val q = SQLQuery().from(cls).where(w)
		return this.count(q)
	}

	fun <T : Model> findByKey(cls: KClass<T>, value: Any): T? {
		val pk = cls.modelPropPrimaryKey ?: return null
		return this.findOne(cls, pk EQ value)
	}

	fun <T : Model> findAll(cls: KClass<T>, w: Where?, block: SQLQuery.() -> Unit): List<T> {
		return this.findAll(cls) {
			where(w)
			this.block()
		}
	}

	fun <T : Model> findOne(cls: KClass<T>, w: Where?): T? {
		return this.findOne(cls) {
			where(w)
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Model> findAll(cls: KClass<T>, block: SQLQuery.() -> Unit): List<T> {
		val ls = this.findObjects {
			from(cls)
			this.block()
		}
		val items = ArrayList<T>()
		ls.forEach {
			val m: T = cls.createYsonModel(it)
			items += m
		}
		return items

	}

	@Suppress("UNCHECKED_CAST")
	fun <T : Model> findOne(cls: KClass<T>, block: SQLQuery.() -> Unit): T? {
		val ls = this.findObjects {
			from(cls)
			limit(1)
			this.block()
		}
		val m: YsonObject = ls.firstOrNull() ?: return null
		return cls.createYsonModel(m)

	}

	fun findRows(block: dev.entao.appbase.sql.SQLQuery.() -> Unit): List<RowData> {
		val q = SQLQuery()
		q.block()
		val c = this.query(q) ?: return emptyList()
		return c.listRow_

	}

	fun findObjects(block: SQLQuery.() -> Unit): List<YsonObject> {
		val q = SQLQuery()
		q.block()
		val c = this.query(q) ?: return emptyList()
		return c.listObject_

	}

	@SuppressLint("Recycle")
	fun count(q: SQLQuery): Int {
		val c = db.rawQuery(q.toCountSQL(), q.sqlArgs) ?: return 0
		c.use { cc ->
			if (cc.moveToNext()) {
				return cc.getInt(0)
			}
		}
		return 0
	}

	fun query(q: SQLQuery): Cursor? {
		return db.rawQuery(q.toSQL(), q.sqlArgs)
	}

	fun dump(cls: KClass<*>) {
		TableCreatorX.check(db, cls)
		db.dumpTable(cls.nameClass)
	}


}