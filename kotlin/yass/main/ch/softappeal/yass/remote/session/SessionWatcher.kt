package ch.softappeal.yass.remote.session

import java.util.concurrent.*

/**
 * Closes a session if it isn't healthy.
 * [executor] is used twice; must interrupt it's threads to terminate checks,
 * checks are also terminated if session is closed.
 * [sessionChecker] must execute without an exception within timeout if session is ok.
 */
@JvmOverloads
fun watchSession(
    executor: Executor, session: Session,
    intervalSeconds: Long, timeoutSeconds: Long, delaySeconds: Long = 0L,
    sessionChecker: () -> Unit
) {
    require(intervalSeconds >= 1)
    require(timeoutSeconds >= 1)
    require(delaySeconds >= 0)
    executor.execute {
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
}
