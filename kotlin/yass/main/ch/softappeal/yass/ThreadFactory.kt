package ch.softappeal.yass

import java.lang.Thread.UncaughtExceptionHandler
import java.util.Date
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

val StdErr = UncaughtExceptionHandler { thread, throwable ->
    try {
        System.err.println("### UncaughtExceptionHandler at '${Date()}' in thread '${thread?.name}' ###")
        throwable?.printStackTrace()
    } finally {
        if (throwable !is Exception) System.exit(1)
    }
}

val Terminate = UncaughtExceptionHandler { thread, throwable ->
    try {
        StdErr.uncaughtException(thread, throwable)
    } finally {
        System.exit(1)
    }
}

@JvmOverloads
fun namedThreadFactory(
    name: String,
    uncaughtExceptionHandler: UncaughtExceptionHandler,
    priority: Int = Thread.NORM_PRIORITY,
    daemon: Boolean = false
): ThreadFactory {
    val number = AtomicInteger(1)
    return ThreadFactory { r ->
        val thread = Thread(r, "$name-${number.getAndIncrement()}")
        thread.uncaughtExceptionHandler = uncaughtExceptionHandler
        if (thread.priority != priority) thread.priority = priority
        if (thread.isDaemon != daemon) thread.isDaemon = daemon
        thread
    }
}
