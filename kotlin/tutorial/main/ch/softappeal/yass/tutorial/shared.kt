package ch.softappeal.yass.tutorial.shared

import ch.softappeal.yass.Interceptor
import ch.softappeal.yass.dump
import ch.softappeal.yass.graphDumper
import ch.softappeal.yass.remote.AbstractInvocation
import ch.softappeal.yass.remote.AsyncInterceptor
import ch.softappeal.yass.remote.isOneWay
import ch.softappeal.yass.remote.session.Session
import ch.softappeal.yass.transport.socket.SslSetup
import ch.softappeal.yass.transport.socket.readKeyStore
import ch.softappeal.yass.tutorial.contract.ApplicationException
import ch.softappeal.yass.tutorial.contract.EchoService
import ch.softappeal.yass.tutorial.contract.SystemException
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.util.Date
import java.util.concurrent.TimeUnit

val EchoServiceImpl = object : EchoService {
    override fun echo(value: Any?): Any? {
        if ("throwRuntimeException" == value) throw RuntimeException("throwRuntimeException")
        TimeUnit.SECONDS.sleep(1)
        return value
    }
}

/** Swallows exceptions of oneWay methods (these are logged in [logger]). */
val UnexpectedExceptionHandler: Interceptor = { method, _, invocation ->
    try {
        invocation.invoke()
    } catch (e: Exception) {
        if (method.isOneWay) {
            // swallow exception
        } else {
            if (e is ApplicationException) throw e // pass through contract exception
            throw SystemException(e.javaClass.name + ": " + e.message) // remap unexpected exception to a contract exception
        }
    }
}

val Dumper = graphDumper(true)

enum class Side { Client, Server }

/** Shows how to implement an interceptor. */
fun logger(session: Session?, side: Side): Interceptor {
    fun log(type: String, method: Method, data: Any?) {
        System.out.printf(
            "%tT | %s | %s | %s | %s | %s%n",
            Date(), session ?: "<no-session>", side, type, method.name, Dumper.dump(data)
        )
    }
    return { method, arguments, invocation ->
        log("entry", method, arguments)
        try {
            val result = invocation.invoke()
            log("exit", method, result)
            result
        } catch (e: Exception) {
            log("exception", method, e)
            throw e
        }
    }
}

val AsyncLogger = object : AsyncInterceptor {
    override fun entry(invocation: AbstractInvocation) {
        println("entry ${invocation.hashCode()}: ${invocation.methodMapping.method.name} ${Dumper.dump(invocation.arguments)}")
    }

    override fun exit(invocation: AbstractInvocation, result: Any?) {
        println("exit ${invocation.hashCode()}: ${invocation.methodMapping.method.name} ${Dumper.dump(result)}")
    }

    override fun exception(invocation: AbstractInvocation, exception: Exception) {
        println("exception ${invocation.hashCode()}: ${invocation.methodMapping.method.name} $exception")
    }
}

private val Password = "StorePass".toCharArray()

private fun keyStore(name: String): KeyStore =
    Files.newInputStream(Paths.get("certificates", name)).use { keyStore -> return readKeyStore(keyStore, Password) }

private fun sslSetup(keyStore: String, trustStore: String): SslSetup =
    SslSetup("TLSv1.2", "TLS_RSA_WITH_AES_128_CBC_SHA", keyStore(keyStore), Password, keyStore(trustStore))

val SslServer = sslSetup("Server.key.pkcs12", "ClientCA.cert.pkcs12")
val SslClient = sslSetup("Client.key.pkcs12", "Server.cert.pkcs12")
