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

private fun isConnected(session: Session?): Boolean =
    (session != null) && !session.isClosed

abstract class ProxyDelegate<S : Session> {
    @Volatile
    private var _session: S? = null

    val session: S
        get() {
            if (!isConnected(_session)) throw SessionClosedException()
            return _session!!
        }

    protected fun setSession(session: S?) {
        _session = session
    }

    val isConnected get() = isConnected(_session)

    /** $todo: Only needed for Java. */
    fun <C : Any> proxy(contract: Class<C>, proxyGetter: (session: S) -> C): C =
        proxy(contract, InvocationHandler { _, method, arguments -> invoke(method, proxyGetter(session), args(arguments)) })

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
            val session = requireNotNull(sessionFactory())
            @Suppress("UNCHECKED_CAST") setSession(session as S)
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
                    setSession(null)
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
