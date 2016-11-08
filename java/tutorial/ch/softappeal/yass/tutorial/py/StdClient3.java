package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Stopwatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class StdClient3 {

    public static void start(final String pythonPgm, final String pythonDirectory) throws IOException {
        final Stopwatch stopwatch = new Stopwatch();
        final Process process = new ProcessBuilder(pythonPgm, "-u", "-m", "tutorial.std_server").directory(new File(pythonDirectory)).start();
        final BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        final ExecutorService stderr = Executors.newSingleThreadExecutor(new NamedThreadFactory("stderr", Exceptions.TERMINATE));
        stderr.execute(() -> {
            try {
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
        final Writer writer = Writer.create(out);
        final Reader reader = Reader.create(process.getInputStream());
        final Serializer messageSerializer = new MessageSerializer(SocketClient.SERIALIZER);
        SocketClient.client(new Client() {
            @Override protected void invoke(final Invocation invocation) throws Exception {
                invocation.invoke(false, request -> {
                    messageSerializer.write(request, writer);
                    out.flush();
                    invocation.settle((Reply)messageSerializer.read(reader));
                });
            }
        });
        stopwatch.stop();
        System.out.println("time: " + stopwatch.milliSeconds() + "ms");
        process.destroyForcibly();
        stderr.shutdownNow();
    }

    public static void main(final String... args) throws IOException {
        start("C:\\Users\\guru\\Miniconda3\\envs\\py3.5\\python.exe", "py3");
    }

}
