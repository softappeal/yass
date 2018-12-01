package ch.softappeal.yass.transport.socket

import java.io.*
import java.net.*
import java.security.*
import javax.net.*
import javax.net.ssl.*

class SslSetup @JvmOverloads constructor(
    protocol: String,
    cipherSuites: List<String>,
    keyStore: KeyStore?,
    keyStorePwd: CharArray?,
    trustStore: KeyStore?,
    random: SecureRandom? = null,
    keyManagerFactoryAlgorithm: String = "SunX509",
    trustManagerFactoryAlgorithm: String = "SunX509"
) {
    val context: SSLContext
    internal val protocols = arrayOf(protocol)
    internal val cipherSuites = cipherSuites.toTypedArray()
    internal val needClientAuth: Boolean

    init {
        check((keyStore != null) || (trustStore != null)) {
            "at least one of keyStore or trustStore must be defined"
        }
        context = SSLContext.getInstance(protocol)
        var keyManagers: Array<KeyManager>? = null
        if (keyStore != null) {
            val keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithm)
            keyManagerFactory.init(keyStore, keyStorePwd)
            keyManagers = keyManagerFactory.keyManagers
        }
        var needClientAuth = false
        var trustManagers: Array<TrustManager>? = null
        if (trustStore != null) {
            val trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithm)
            trustManagerFactory.init(trustStore)
            trustManagers = trustManagerFactory.trustManagers
            needClientAuth = true
        }
        context.init(keyManagers, trustManagers, random)
        this.needClientAuth = needClientAuth
    }
}

val SslSetup.socketFactory: SocketFactory
    get() = object : AbstractSocketFactory() {
        override fun createSocket(): Socket {
            val socket = context.socketFactory.createSocket() as SSLSocket
            try {
                socket.enabledProtocols = protocols
                socket.enabledCipherSuites = cipherSuites
            } catch (e: Exception) {
                close(socket, e)
                throw e
            }
            return socket
        }
    }

val SslSetup.serverSocketFactory: ServerSocketFactory
    get() = object : AbstractServerSocketFactory() {
        override fun createServerSocket(): ServerSocket {
            val serverSocket = context.serverSocketFactory.createServerSocket() as SSLServerSocket
            try {
                serverSocket.needClientAuth = needClientAuth
                serverSocket.enabledProtocols = protocols
                serverSocket.enabledCipherSuites = cipherSuites
            } catch (e: Exception) {
                close(serverSocket, e)
                throw e
            }
            return serverSocket
        }
    }

@JvmOverloads
fun readKeyStore(keyStore: InputStream, keyStorePwd: CharArray? = null, keyStoreType: String = "PKCS12"): KeyStore {
    val ks = KeyStore.getInstance(keyStoreType)
    ks.load(keyStore, keyStorePwd)
    return ks
}
