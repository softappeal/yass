package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocalConnectionTest extends SessionTest {

    @Test public void plain1() throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            LocalConnection.connect(invokeSessionFactory(true, false, executor), invokeSessionFactory(false, false, executor));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void plain2() throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            LocalConnection.connect(invokeSessionFactory(false, false, executor), invokeSessionFactory(true, false, executor));
            TimeUnit.MILLISECONDS.sleep(400L);
        } finally {
            executor.shutdown();
        }
    }

    @Test public void createException() {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            try {
                LocalConnection.connect(invokeSessionFactory(false, false, executor), invokeSessionFactory(false, true, executor));
                Assert.fail();
            } catch (final RuntimeException e) {
                Assert.assertEquals(e.getMessage(), "java.lang.Exception: create failed");
            }
        } finally {
            executor.shutdown();
        }
    }

}
