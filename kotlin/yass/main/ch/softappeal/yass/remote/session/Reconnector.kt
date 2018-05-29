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
    private var session: S? = null

    protected fun session(session: S?) {
        this.session = session
    }

    private fun connected(session: Session?): Boolean = (session != null) && !session.isClosed

    fun connected() = connected(session)

    fun session(): S {
        val session = this.session
        if (!connected(session)) throw SessionClosedException()
        return session!!
    }

    fun <C : Any> proxy(contract: Class<C>, proxyGetter: (session: S) -> C): C =
        proxy(contract, InvocationHandler { _, method, arguments -> invoke(method, proxyGetter(session()) as Any, args(arguments)) })

}

/** Thrown exceptions will be ignored. */
typealias Connector = (sessionFactory: SessionFactory) -> Unit

/** Provides proxies surviving reconnects. */
open class Reconnector<S : Session> : ProxyDelegate<S>() {

    /** [executor] is called once; must interrupt it's threads to terminate reconnects. */
    @JvmOverloads
    fun start(executor: Executor, intervalSeconds: Long, sessionFactory: SessionFactory, connector: Connector, delaySeconds: Long = 0) {
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
                if (!connected()) {
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
