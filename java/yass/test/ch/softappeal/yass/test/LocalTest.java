package ch.softappeal.yass.test;

import ch.softappeal.yass.Interceptor;
import org.junit.Test;

public class LocalTest extends InvokeTest {

    @Test public void test() throws InterruptedException {
        invoke(
            Interceptor.proxy(TestService.class, new TestServiceImpl(), PRINTLN_AFTER, CLIENT_INTERCEPTOR, SERVER_INTERCEPTOR)
        );
    }

}
