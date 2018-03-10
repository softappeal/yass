package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.InputStreamSupplier;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class InputStreamSupplierTest {

    @SuppressWarnings("try")
    @Test public void file() throws IOException {
        try (var inputStream = InputStreamSupplier.create(
            "test/" + InputStreamSupplierTest.class.getName().replace('.', '/') + ".java"
        ).get()) {
            // empty
        }
    }

    @Test public void fileFailed() throws IOException {
        try {
            InputStreamSupplier.create(
                "INVALID"
            ).get();
            Assert.fail();
        } catch (final RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }

    @SuppressWarnings("try")
    @Test public void classLoader() throws IOException {
        try (var inputStream = InputStreamSupplier.create(
            InputStreamSupplier.class.getClassLoader(),
            InputStreamSupplierTest.class.getName().replace('.', '/') + ".class"
        ).get()) {
            // empty
        }
    }

    @Test public void classLoaderFailed() throws IOException {
        try {
            InputStreamSupplier.create(
                InputStreamSupplier.class.getClassLoader(),
                "INVALID"
            ).get();
            Assert.fail();
        } catch (final RuntimeException e) {
            Assert.assertEquals("resource 'INVALID' not found", e.getMessage());
        }
    }

}
