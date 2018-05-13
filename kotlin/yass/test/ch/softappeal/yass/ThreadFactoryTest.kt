package ch.softappeal.yass

import org.junit.Test
import kotlin.test.Ignore

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

}
