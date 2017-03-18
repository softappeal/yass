package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.InputStreamSupplier;
import ch.softappeal.yass.util.Resource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamSupplierTest {

    @SuppressWarnings("try")
    @Test public void file() throws IOException {
        try (InputStream inputStream = InputStreamSupplier.create(
            "java/test/" + InputStreamSupplierTest.class.getName().replace('.', '/') + ".java"
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
        try (InputStream inputStream = InputStreamSupplier.create(
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

    void useResource(final Resource resource) {
        // empty
    }

    @SuppressWarnings("deprecation")
    void convert() {
        useResource(Resource.convert(InputStreamSupplier.create("dummy")));
    }

}
