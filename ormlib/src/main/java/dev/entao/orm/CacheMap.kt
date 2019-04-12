@file:Suppress("unused")

package dev.entao.orm

class CacheMap<K, V>(val onMissing: (K) -> V?) {

	val map = HashMap<K, V?>()

	@Synchronized
	fun get(key: K): V? {
		if (!map.containsKey(key)) {
			val v = onMissing(key)
			map[key] = v
		}
		return map[key]
	}

	@Synchronized
	fun remove(key: K): V? {
		return map.remove(key)
	}
}