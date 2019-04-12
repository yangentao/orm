@file:Suppress("unused")

package dev.entao.orm

import android.database.sqlite.SQLiteDatabase
import dev.entao.appbase.sql.*
import dev.entao.base.*
import org.jetbrains.annotations.NotNull
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.findAnnotation

/**
 * Created by entaoyang@163.com on 2017-03-07.
 */


private val KMutableProperty<*>.uniqueName: String
	get() {
		return this.findAnnotation<Unique>()?.name ?: ""
	}

//TODO booean 是int还是string ?
private val KProperty<*>.sqlTypeName: String
	get() {
		if (this.isTypeBoolean || this.isTypeByte || this.isTypeShort || this.isTypeInt || this.isTypeLong) {
			return "INTEGER"
		}
		if (this.isTypeFloat || this.isTypeDouble) {
			return "REAL"
		}
		if (this.isTypeByteArray) {
			return "BLOB"
		}
		return "TEXT"
	}

private fun KMutableProperty<*>.defineColumn(defPK: Boolean): String {
	val sb = StringBuilder(64)
	sb.append(this.nameProp).append(" ").append(this.sqlTypeName)
	val length: Int = this.findAnnotation<Length>()?.value ?: 0
	if (length > 0) {
		sb.append("($length) ")
	}
	if (defPK && this.isPrimaryKey) {
		sb.append(" PRIMARY KEY ")
		if (this.hasAnnotation<AutoInc>()) {
			sb.append(" AUTOINCREMENT ")
		}
	}
	if (this.hasAnnotation<NotNull>()) {
		sb.append(" NOT NULL ")
	}
	val unique = this.findAnnotation<Unique>()
	if (unique != null) {
		if (unique.name.isEmpty()) {
			sb.append(" UNIQUE ")
		}
	}
	return sb.toString()

}

object TableCreatorX {
	val checkedSet = HashSet<String>()

	fun check(db: SQLiteDatabase, cls: KClass<*>) {
		synchronized(checkedSet) {
			val k = db.path + "@" + cls.nameClass
			if (k in checkedSet) {
				return
			}
			checkedSet.add(k)
			doCheck(db, cls)
		}

	}

	private fun doCheck(db: SQLiteDatabase, cls: KClass<*>) {
		if (db.existTable(cls.nameClass)) {
			db.trans {
				checkTable(db, cls)
				checkIndex(db, cls)
			}
		} else {
			db.trans {
				createTable(db, cls)
				createIndex(db, cls)
			}
		}
	}

	private fun checkTable(L: SQLiteDatabase, cls: KClass<*>) {
		if (!cls.autoAlterTable) {
			return
		}
		val set = L.tableInfo(cls.nameClass).map { it.name }.toSet()
		for (p in cls.modelPropList) {
			if (p.nameProp !in set) {
				L.addColumn(cls.nameClass, p.defineColumn(true))
			}
		}
	}
	private fun indexNameOf(table: String, vararg cols: String): String {
		val s1 = mutableListOf(*cols).sorted().joinToString("_")
		return "${table}_$s1"
	}
	private fun checkIndex(L: SQLiteDatabase, cls: KClass<*>) {
		val set = L.indexsOf(cls.nameClass)
		for (p in cls.modelPropList) {
			if (p.isPrimaryKey || p.hasAnnotation<Unique>() || !p.hasAnnotation<Index>()) {
				continue
			}
			val indexName = indexNameOf(cls.nameClass, p.nameProp)
			if (indexName !in set) {
				L.createIndex(cls.nameClass, p.nameProp)
			}
		}
	}

	private fun createTable(L: SQLiteDatabase, cls: KClass<*>) {
		val ls = ArrayList<String>(12)
		val pkCols = cls.modelPropPrimaryKeyList
		cls.modelPropList.mapTo(ls) { it.defineColumn(pkCols.size < 2) }
		if (pkCols.size >= 2) {
			val s = pkCols.joinToString(",") { it.nameProp }
			ls.add("PRIMARY KEY ($s)")
		}

		val uMap = cls.modelPropList.filter { it.hasAnnotation<Unique>() && it.uniqueName.isNotEmpty() }.groupBy { it.uniqueName }
		for ((k, v) in uMap) {
			val cs = v.joinToString(",") { it.nameProp }
			ls.add("CONSTRAINT $k UNIQUE ($cs)")
		}

		val us = cls.findAnnotation<Uniques>()?.value
		if (us != null && us.isNotEmpty()) {
			val s = "CONSTRAINT " + us.joinToString("_") + " UNIQUE (" + us.joinToString(",") + ")"
			ls.add(s)
		}
		L.createTable(cls.nameClass, ls)
	}

	private fun createIndex(L: SQLiteDatabase, cls: KClass<*>) {
		for (p in cls.modelPropList) {
			if (p.isPrimaryKey || p.hasAnnotation<Unique>() || !p.hasAnnotation<Index>()) {
				continue
			}
			L.createIndex(cls.nameClass, p.nameProp)
		}
	}


}