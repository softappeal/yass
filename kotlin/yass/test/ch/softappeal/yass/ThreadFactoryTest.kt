package ch.softappeal.yass

import org.junit.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals

class ThreadFactoryTest {
    @Test
    fun stdErrOk() {
        StdErr.uncaughtException(null, Exception("test"))
        StdErr.uncaughtException(Thread.currentThread(), Exception("test"))
    }

    @Test
    @Ignore
    fun stdErrFail() {
        StdErr.uncaughtException(null, null)
    }

    @Test
    @Ignore
    fun terminateFail() {
        Terminate.uncaughtException(Thread.currentThread(), Exception("test"))
    }

    @Test
    fun namedThreadFactory() {
        val threadFactory = namedThreadFactory("test", Terminate)
        var thread = threadFactory.newThread {
            val t = Thread.currentThread()
            assertEquals("test-1", t.name)
            assertEquals(Thread.NORM_PRIORITY, t.priority)
            assertEquals(false, t.isDaemon)
        }
        thread.start()
        thread.join()
        thread = threadFactory.newThread {}
        thread.start()
        thread.join()
        assertEquals("test-2", thread.name)
    }
}
