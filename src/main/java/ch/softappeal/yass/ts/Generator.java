package ch.softappeal.yass.ts;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

abstract class Generator {

    private final PrintWriter printer;

    protected final void print(final String format, final Object... args) {
        printer.format(format, args);
    }

    protected final void println() {
        printer.print('\n');
    }

    protected final void println(final String format, final Object... args) {
        print(format, args);
        println();
    }

    private int tabs = 0;

    protected final void inc() {
        tabs++;
    }

    protected final void dec() {
        if (tabs <= 0) {
            throw new IllegalStateException();
        }
        tabs--;
    }

    protected final void tab() {
        printer.print("    ");
    }

    protected final void tabs() {
        for (int t = 0; t < tabs; t++) {
            tab();
        }
    }

    protected final void tabs(final String format, final Object... args) {
        tabs();
        print(format, args);
    }

    protected final void tabsln(final String format, final Object... args) {
        tabs(format, args);
        println();
    }

    protected final void close() throws IOException {
        printer.close();
        if (printer.checkError()) { // needed because PrintWriter doesn't throw IOException
            throw new IOException();
        }
    }

    protected Generator(final String file) throws IOException {
        printer = new PrintWriter(file, StandardCharsets.UTF_8.name());
    }

}
