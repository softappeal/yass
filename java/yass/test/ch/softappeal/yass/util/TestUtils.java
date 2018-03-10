package ch.softappeal.yass.util;

import org.junit.Assert;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class TestUtils {

    @FunctionalInterface public interface Printer {
        void print(PrintWriter printer) throws Exception;
    }

    public static void compareFile(final String file, final Printer printer) {
        try {
            final var writer = new PrintWriter(System.out);
            printer.print(writer);
            writer.flush();
            final var buffer = new CharArrayWriter();
            try (var out = new PrintWriter(buffer)) {
                printer.print(out);
            }
            final var testReader = new BufferedReader(new CharArrayReader(buffer.toCharArray()));
            try (
                var refReader = new BufferedReader(new InputStreamReader(
                    InputStreamSupplier.create("test/" + file).get(),
                    StandardCharsets.UTF_8
                ))
            ) {
                while (true) {
                    final var testLine = testReader.readLine();
                    final var refLine = refReader.readLine();
                    if ((testLine == null) && (refLine == null)) {
                        return;
                    }
                    if ((testLine == null) || (refLine == null)) {
                        throw new RuntimeException("files don't have same length");
                    }
                    Assert.assertEquals(testLine, refLine);
                }
            }
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
