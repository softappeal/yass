package ch.softappeal.yass.transport.socket

import ch.softappeal.yass.Terminate
import ch.softappeal.yass.remote.CalculatorImpl
import ch.softappeal.yass.remote.Server
import ch.softappeal.yass.remote.Service
import ch.softappeal.yass.remote.calculatorId
import ch.softappeal.yass.remote.session.useExecutor
import ch.softappeal.yass.remote.useClient
import ch.softappeal.yass.transport.ClientSetup
import ch.softappeal.yass.transport.ServerSetup
import java.lang.reflect.UndeclaredThrowableException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import javax.net.ServerSocketFactory
import javax.net.SocketFactory
import javax.net.ssl.SSLSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

private fun test(
    serverSocketFactory: ServerSocketFactory, socketFactory: SocketFactory, needClientAuth: Boolean,
    uncaughtExceptionHandler: Thread.UncaughtExceptionHandler = Terminate
) {
    fun checkName(name: String) {
        println("checkName")
        assertEquals(name, (socket as SSLSocket).session.peerPrincipal.name)
    }
    useExecutor(uncaughtExceptionHandler) { executor, done ->
        socketServer(ServerSetup(Server(Service(calculatorId, CalculatorImpl, { _, _, invocation ->
            if (needClientAuth) checkName("CN=Client")
            invocation()
        })), messageSerializer), executor)
            .start(executor, socketBinder(address, serverSocketFactory)).use {
                useClient(
                    socketClient(ClientSetup(messageSerializer), socketConnector(address, socketFactory))
                        .proxy(calculatorId, { _, _, invocation ->
                            checkName("CN=Server")
                            invocation()
                        })
                )
            }
        done()
    }
}

private fun failedTest(
    serverSocketFactory: ServerSocketFactory,
    socketFactory: SocketFactory,
    needClientAuth: Boolean
) {
    try {
        test(
            serverSocketFactory, socketFactory, needClientAuth,
            Thread.UncaughtExceptionHandler { _, e ->
                e.printStackTrace()
            }
        )
        fail()
    } catch (ignore: UndeclaredThrowableException) {
    }
}

private val PASSWORD = "StorePass".toCharArray()

private fun readKeyStore(name: String): KeyStore {
    return Files.newInputStream(Paths.get("../../certificates", name)).use { readKeyStore(it, PASSWORD) }
}

private val SERVER_KEY = readKeyStore("Server.key.pkcs12")
private val SERVER_CERT = readKeyStore("Server.cert.pkcs12")
private val CLIENTCA_CERT = readKeyStore("ClientCA.cert.pkcs12")
private val CLIENT_KEY = readKeyStore("Client.key.pkcs12")

private const val PROTOCOL = "TLSv1.2"
private val CIPHER_SUITES = listOf("TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_RSA_WITH_AES_256_CBC_SHA")

class SslTest {
    @Test
    fun onlyServerAuthentication() {
        test(
            SslSetup(PROTOCOL, CIPHER_SUITES, SERVER_KEY, PASSWORD, null).serverSocketFactory,
            SslSetup(PROTOCOL, CIPHER_SUITES, null, null, SERVER_CERT).socketFactory,
            false
        )
    }

    @Test
    fun clientAndServerAuthentication() {
        test(
            SslSetup(PROTOCOL, CIPHER_SUITES, SERVER_KEY, PASSWORD, CLIENTCA_CERT).serverSocketFactory,
            SslSetup(PROTOCOL, CIPHER_SUITES, CLIENT_KEY, PASSWORD, SERVER_CERT).socketFactory,
            true
        )
    }

    @Test
    fun wrongServer() {
        failedTest(
            SslSetup(PROTOCOL, CIPHER_SUITES, SERVER_KEY, PASSWORD, null).serverSocketFactory,
            SslSetup(PROTOCOL, CIPHER_SUITES, null, null, CLIENTCA_CERT).socketFactory,
            false
        )
    }

    @Test
    fun wrongClientCA() {
        failedTest(
            SslSetup(PROTOCOL, CIPHER_SUITES, SERVER_KEY, PASSWORD, SERVER_CERT).serverSocketFactory,
            SslSetup(PROTOCOL, CIPHER_SUITES, CLIENT_KEY, PASSWORD, SERVER_CERT).socketFactory,
            true
        )
    }
}
