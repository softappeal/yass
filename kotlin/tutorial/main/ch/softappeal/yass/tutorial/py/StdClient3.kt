package ch.softappeal.yass.tutorial.py

import ch.softappeal.yass.Terminate
import ch.softappeal.yass.namedThreadFactory
import ch.softappeal.yass.remote.Client
import ch.softappeal.yass.remote.ClientInvocation
import ch.softappeal.yass.remote.Reply
import ch.softappeal.yass.serialize.reader
import ch.softappeal.yass.serialize.writer
import ch.softappeal.yass.transport.messageSerializer
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

fun start(pythonPgm: String, pythonDirectory: String) {
    println("time: ${measureTimeMillis {
        val process = ProcessBuilder(pythonPgm, "-u", "-m", "tutorial.std_server").directory(File(pythonDirectory)).start()
        val stderr = Executors.newSingleThreadExecutor(namedThreadFactory("stderr", Terminate))
        stderr.execute {
            BufferedReader(InputStreamReader(process.errorStream, StandardCharsets.UTF_8)).use { err ->
                while (true) {
                    val s = err.readLine()
                    if (s != null) System.err.println("<python process stderr> $s")
                }
            }
        }
        val out = process.outputStream
        val writer = writer(out)
        val reader = reader(process.inputStream)
        val messageSerializer = messageSerializer(Serializer)
        client(object : Client() {
            override fun invoke(invocation: ClientInvocation) {
                invocation.invoke(false) { request ->
                    messageSerializer.write(writer, request)
                    out.flush()
                    invocation.settle(messageSerializer.read(reader) as Reply)
                }
            }
        })
        process.destroyForcibly()
        stderr.shutdownNow()
    }}ms")
}

fun main(args: Array<String>) {
    start("C:/Users/guru/Miniconda3/envs/py3/python.exe", "py3")
}
