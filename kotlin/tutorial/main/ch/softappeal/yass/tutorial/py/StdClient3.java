package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.remote.Client;
import ch.softappeal.yass.remote.ClientInvocation;
import ch.softappeal.yass.remote.Reply;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ch.softappeal.yass.ThreadFactoryKt.getTerminate;
import static ch.softappeal.yass.ThreadFactoryKt.namedThreadFactory;
import static ch.softappeal.yass.serialize.ReaderKt.reader;
import static ch.softappeal.yass.serialize.WriterKt.writer;
import static ch.softappeal.yass.transport.MessageSerializerKt.messageSerializer;

public final class StdClient3 {

    public static void start(final String pythonPgm, final String pythonDirectory) throws IOException {
        final long start = System.currentTimeMillis();
        final Process process = new ProcessBuilder(pythonPgm, "-u", "-m", "tutorial.std_server").directory(new File(pythonDirectory)).start();
        final ExecutorService stderr = Executors.newSingleThreadExecutor(namedThreadFactory("stderr", getTerminate()));
        stderr.execute(() -> {
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                while (true) {
                    final String s = err.readLine();
                    if (s == null) {
                        return;
                    }
                    System.err.println("<python process stderr> " + s);
                }
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
        final OutputStream out = process.getOutputStream();
        final Writer writer = writer(out);
        final Reader reader = reader(process.getInputStream());
        final Serializer messageSerializer = messageSerializer(SocketClient.SERIALIZER);
        SocketClient.client(new Client() {
            @Override protected void invoke(final ClientInvocation invocation) throws Exception {
                invocation.invoke(false, request -> {
                    try {
                        messageSerializer.write(writer, request);
                        out.flush();
                        invocation.settle((Reply)messageSerializer.read(reader));
                    } catch (final Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }
        });
        final long stop = System.currentTimeMillis();
        System.out.println("time: " + (stop - start) + "ms");
        process.destroyForcibly();
        stderr.shutdownNow();
    }

    public static void main(final String... args) throws IOException {
        start("C:\\Users\\guru\\Miniconda3\\envs\\py3\\python.exe", "py3");
    }

}
