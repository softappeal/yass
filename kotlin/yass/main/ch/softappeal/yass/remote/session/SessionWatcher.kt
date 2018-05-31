@file:JvmName("Kt")
@file:JvmMultifileClass

package ch.softappeal.yass.remote.session

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Closes a session if it isn't healthy.
 * [executor] is used twice; must interrupt it's threads to terminate checks, checks are also terminated if session is closed.
 * [sessionChecker] must execute without an exception within timeout if session is ok.
 */
fun watchSession(
    executor: Executor, session: Session,
    intervalSeconds: Long, timeoutSeconds: Long, delaySeconds: Long = 0L,
    sessionChecker: () -> Unit
): Unit = executor.execute {
    try {
        TimeUnit.SECONDS.sleep(delaySeconds)
    } catch (e: InterruptedException) {
        return@execute
    }
    while (!session.isClosed && !Thread.interrupted()) {
        try {
            TimeUnit.SECONDS.sleep(intervalSeconds)
        } catch (e: InterruptedException) {
            return@execute
        }
        val ok = CountDownLatch(1)
        executor.execute {
            try {
                if (!ok.await(timeoutSeconds, TimeUnit.SECONDS)) session.close(Exception("sessionChecker"))
            } catch (ignore: InterruptedException) {
            }
        }
        try {
            sessionChecker()
        } catch (e: Exception) {
            session.close(e)
            return@execute
        }
        ok.countDown()
    }
}
