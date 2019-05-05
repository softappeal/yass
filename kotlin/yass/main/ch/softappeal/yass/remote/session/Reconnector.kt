package ch.softappeal.yass.remote.session

import ch.softappeal.yass.*
import java.lang.reflect.*
import java.util.concurrent.*

private fun isConnected(session: Session?): Boolean =
    (session != null) && !session.isClosed

abstract class ProxyDelegate<S : Session> {
    @Volatile
    private var _session: S? = null

    val session: S
        get() {
            if (!isConnected) throw SessionClosedException()
            return _session!!
        }

    protected fun setSession(session: S?) {
        _session = session
    }

    val isConnected get() = isConnected(_session)

    @OnlyNeededForJava
    fun <C : Any> proxy(contract: Class<C>, proxyGetter: (session: S) -> C): C =
        proxy(
            contract,
            InvocationHandler { _, method, arguments -> invoke(method, proxyGetter(session), args(arguments)) }
        )

    inline fun <reified C : Any> proxy(noinline proxyGetter: (session: S) -> C): C =
        proxy(C::class.java, proxyGetter)
}

/** Provides proxies surviving reconnects. */
open class Reconnector<S : Session> : ProxyDelegate<S>() {
    /**
     * [executor] is called once; must interrupt it's threads to terminate reconnects.
     * Thrown exceptions of [connector] will be ignored.
     */
    @JvmOverloads
    fun start(
        executor: Executor,
        intervalSeconds: Long, sessionFactory: SessionFactory, delaySeconds: Long = 0,
        connector: (sessionFactory: SessionFactory) -> Unit
    ) {
        require(intervalSeconds >= 1)
        require(delaySeconds >= 0)
        val reconnectorSessionFactory = {
            val session = sessionFactory()
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
