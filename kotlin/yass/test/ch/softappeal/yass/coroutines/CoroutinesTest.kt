package ch.softappeal.yass.coroutines

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture

fun main() {
    GlobalScope.launch {
        // launch new coroutine in background and continue
        delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
        println("World!") // print after delay
    }
    println("Hello,") // main thread continues while coroutine is delayed
    Thread.sleep(2000L) // block main thread for 2 seconds to keep JVM alive

    // Let's assume that we have a future coming from some 3rd party API
    val future: CompletableFuture<Int> = CompletableFuture.supplyAsync {
        Thread.sleep(1000L) // imitate some long-running computation, actually
        42
    }
    // now let's launch a coroutine and await for this future inside it
    runBlocking {
        println("We can do something else, while we are waiting for future...")
        println("We've got ${future.await()} from the future!")
    }
}
