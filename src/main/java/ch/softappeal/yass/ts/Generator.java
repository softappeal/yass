package ch.softappeal.yass.ts;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

abstract class Generator {

    private final PrintWriter printer;

    final void print(final String format, final Object... args) {
        printer.format(format, args);
    }

    final void println() {
        printer.print('\n');
    }

    final void println(final String format, final Object... args) {
        print(format, args);
        println();
    }

    private int tabs = 0;

    final void inc() {
        tabs++;
    }

    final void dec() {
        if (tabs <= 0) {
            throw new IllegalStateException();
        }
        tabs--;
    }

    final void tab() {
        printer.print("    ");
    }

    final void tabs() {
        for (int t = 0; t < tabs; t++) {
            tab();
        }
    }

    final void tabs(final String format, final Object... args) {
        tabs();
        print(format, args);
    }

    final void tabsln(final String format, final Object... args) {
        tabs(format, args);
        println();
    }

    final void includeFile(final String file) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            while (true) {
                final String s = in.readLine();
                if (s == null) {
                    break;
                }
                println(s);
            }
        }
    }

    final void close() throws IOException {
        printer.close();
        if (printer.checkError()) { // needed because PrintWriter doesn't throw IOException
            throw new IOException();
        }
    }

    Generator(final String file) throws IOException {
        printer = new PrintWriter(file, StandardCharsets.UTF_8.name());
    }

}
