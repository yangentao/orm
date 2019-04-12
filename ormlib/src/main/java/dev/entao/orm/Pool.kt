@file:Suppress("unused")

package dev.entao.orm

import dev.entao.appbase.App
import dev.entao.base.closeSafe
import java.util.*


val PoolPeek: Conn get() = Pool.peek

object Pool {
	private val globalConn = Conn(App.openOrCreateDatabase("global_database"))

	private val stackLocal = object : ThreadLocal<Stack<Conn>>() {
		override fun initialValue(): Stack<Conn> {
			return Stack()
		}
	}
	val peek: Conn
		get() {
			val stack = stackLocal.get()
			if (stack.isNotEmpty()) {
				return stack.peek()
			}
			return globalConn
		}

	@Synchronized
	fun <R> named(name: String, block: (Conn) -> R): R {
		val db = App.openOrCreateDatabase(name)
		val c = Conn(db)
		stackLocal.get().push(c)
		try {
			return block(c)
		} catch (t: Throwable) {
			throw t
		} finally {
			stackLocal.get().pop()
			c.closeSafe()
		}
	}

	fun <R> hex(user: String, block: (Conn) -> R): R {
		val a = user.map { it.toInt().toString(16) }.joinToString("")
		return named("$a.db", block)
	}

	@Synchronized
	fun <R> global(block: (Conn) -> R): R {
		stackLocal.get().push(globalConn)
		try {
			return block(globalConn)
		} finally {
			stackLocal.get().pop()

		}
	}


}