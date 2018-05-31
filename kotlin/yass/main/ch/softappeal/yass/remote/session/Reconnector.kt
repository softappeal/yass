@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote.session

import ch.softappeal.yass.args
import ch.softappeal.yass.invoke
import ch.softappeal.yass.proxy
import ch.softappeal.yass.transport.SessionFactory
import java.lang.reflect.InvocationHandler
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

abstract class ProxyDelegate<S : Session> {
    @Volatile
    private var _session: S? = null

    val session: S
        get() {
            if (!connected(_session)) throw SessionClosedException()
            return _session!!
        }

    protected fun session(session: S?) {
        this._session = session
    }

    private fun connected(session: Session?): Boolean =
        (session != null) && !session.isClosed

    val isConnected get() = connected(_session)

    fun <C : Any> proxy(contract: Class<C>, proxyGetter: (session: S) -> C): C =
        proxy(contract, InvocationHandler { _, method, arguments -> invoke(method, proxyGetter(session) as Any, args(arguments)) })

    inline fun <reified C : Any> proxy(noinline proxyGetter: (session: S) -> C): C =
        proxy(C::class.java, proxyGetter)
}

/** Provides proxies surviving reconnects. */
open class Reconnector<S : Session> : ProxyDelegate<S>() {
    /**
     * [executor] is called once; must interrupt it's threads to terminate reconnects.
     * Thrown exceptions of [connector] will be ignored.
     */
    fun start(
        executor: Executor,
        intervalSeconds: Long, sessionFactory: SessionFactory, delaySeconds: Long = 0,
        connector: (sessionFactory: SessionFactory) -> Unit
    ) {
        val reconnectorSessionFactory = {
            val session = sessionFactory()
            @Suppress("UNCHECKED_CAST") session(session as S)
            session
        }
        executor.execute {
            try {
                TimeUnit.SECONDS.sleep(delaySeconds)
            } catch (e: InterruptedException) {
                return@execute
            }
            while (!Thread.interrupted()) {
                if (!isConnected) {
                    session(null)
                    try {
                        connector(reconnectorSessionFactory)
                    } catch (ignore: Exception) {
                    }
                }
                try {
                    TimeUnit.SECONDS.sleep(intervalSeconds)
                } catch (e: InterruptedException) {
                    return@execute
                }
            }
        }
    }
}
