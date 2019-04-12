@file:Suppress("unused")

package dev.entao.orm
import dev.entao.base.isPublic
import dev.entao.base.nameProp
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties


val KClass<*>.modelPropList: List<KMutableProperty<*>>
	get() {
		return classPropCache.get(this) ?: emptyList()
	}

val KClass<*>.modelPropPrimaryKeyList: List<KMutableProperty<*>>
	get() {
		return this.modelPropList.filter { it.isPrimaryKey }
	}

val KClass<*>.modelPropPrimaryKey: KMutableProperty<*>?
	get() {
		return classPKPropCache.get(this)
	}

val KClass<*>.modelPropKeySet: Set<String>
	get() {
		return classPropKeyCache.get(this) ?: emptySet()
	}


private val classPropKeyCache = CacheMap<KClass<*>, Set<String>> {
	it.modelPropList.map { it.nameProp }.toSet()
}

private val classPropCache = CacheMap<KClass<*>, List<KMutableProperty<*>>> {
	findModelProperties(it)
}
private val classPKPropCache = CacheMap<KClass<*>, KMutableProperty<*>> { cls ->
	cls.modelPropList.find {
		it.isPrimaryKey
	}
}

private fun findModelProperties(cls: KClass<*>): List<KMutableProperty<*>> {
	return cls.memberProperties.filter {
		if (it !is KMutableProperty<*>) {
			false
		} else if (it.isAbstract || it.isConst || it.isLateinit) {
			false
		} else if (!it.isPublic) {
			false
		} else !it.isExcluded
	}.map { it as KMutableProperty<*> }
}