package ch.softappeal.yass.generate;

import ch.softappeal.yass.util.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public abstract class Out {

    private final PrintWriter printer;
    private @Nullable Appendable buffer = null;

    protected final void redirect(final @Nullable Appendable buffer) {
        this.buffer = buffer;
    }

    protected final void print(final CharSequence s) {
        if (buffer != null) {
            try {
                buffer.append(s);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            printer.print(s);
        }
    }

    protected final void print(final String format, final Object... args) {
        print(String.format(format, args));
    }

    protected final void println() {
        print("\n");
    }

    protected final void println2() {
        println();
        println();
    }

    protected final void println(final String format, final Object... args) {
        print(format, args);
        println();
    }

    protected final void println(final CharSequence s) {
        print(s);
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
        print("    ");
    }

    protected final void tabs() {
        for (var t = 0; t < tabs; t++) {
            tab();
        }
    }

    protected final void tabs(final String format, final Object... args) {
        tabs();
        print(format, args);
    }

    protected final void tabs(final CharSequence s) {
        tabs();
        print(s);
    }

    protected final void tabsln(final String format, final Object... args) {
        tabs(format, args);
        println();
    }

    protected final void tabsln(final CharSequence s) {
        tabs(s);
        println();
    }

    protected final void includeFile(final String file) throws IOException {
        try (var in = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            while (true) {
                final var s = in.readLine();
                if (s == null) {
                    break;
                }
                println(s);
            }
        }
    }

    protected final void close() throws IOException {
        printer.close();
        if (printer.checkError()) { // needed because PrintWriter doesn't throw IOException
            throw new IOException();
        }
    }

    protected Out(final String file) throws IOException {
        final var directory = new File(file).getParentFile();
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("directory '" + directory + "' not created");
        }
        printer = new PrintWriter(file, StandardCharsets.UTF_8.name());
    }

}
