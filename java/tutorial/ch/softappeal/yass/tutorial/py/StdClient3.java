package ch.softappeal.yass.tutorial.py;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Reply;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.util.Stopwatch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public final class StdClient3 {

    public static void main(final String... args) throws IOException {
        final Stopwatch stopwatch = new Stopwatch();
        final Process process = new ProcessBuilder("python", "-m", "tutorial.std_server").directory(new File("py3")).start();
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
    }

}