package ch.softappeal.yass.transport.socket.test;

import javax.net.ServerSocketFactory;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class PythonForwarder {

    private static void run(final int port, final String serverUrl) throws Exception {
        final URL url = new URL(serverUrl);
        final Executor requestExecutor = Executors.newCachedThreadPool();
        final ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket();
        serverSocket.bind(new InetSocketAddress("localhost", port));
        System.out.println("started");
        while (true) {
            final Socket socket = serverSocket.accept();
            requestExecutor.execute(new Runnable() {

                void copy(final InputStream in, final int length, final OutputStream out) throws Exception {
                    final byte[] body = new byte[length];
                    new DataInputStream(in).readFully(body);
                    out.write(body);
                }

                void handleRequest(final Socket socket) throws Exception {
                    final HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                    try {
                        socket.setTcpNoDelay(true); // force immediate send
                        connection.setDoOutput(true);
                        connection.setRequestMethod("POST");
                        final InputStream in = socket.getInputStream();
                        copy(in, new DataInputStream(in).readInt(), connection.getOutputStream());
                        copy(connection.getInputStream(), connection.getContentLength(), socket.getOutputStream());
                    } finally {
                        connection.disconnect();
                    }
                }

                @Override public void run() {
                    try {
                        try {
                            handleRequest(socket);
                        } finally {
                            socket.close();
                        }
                    } catch (final Exception ignore) {
                        System.err.println(ignore);
                    }
                }

            });
        }
    }

    public static void main(final String... args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: port url");
        }
        run(Integer.valueOf(args[0]), args[1]);
    }

}

/*

import socket
from typing import Any, cast

from yass import Client, Request, Reply, Writer, Reader, Serializer, MessageSerializer


def socketClient(contractSerializer: Serializer, address: Any) -> Client:
    messageSerializer = MessageSerializer(contractSerializer)

    class SocketClient(Client):
        def invoke(self, request: Request) -> Reply:
            s = socket.socket()
            try:
                s.connect(address)

                def writeBytes(value: bytes) -> None:
                    nonlocal out
                    out += value

                writer = Writer(writeBytes)
                out = b''
                messageSerializer.write(request, writer)
                body = out
                out = b''
                writer.writeInt(len(body))
                s.sendall(out + body)

                def readBytes(length: int) -> bytes:
                    buffer = b''
                    while len(buffer) < length:
                        chunk = s.recv(length - len(buffer))
                        if len(chunk) == 0:
                            raise RuntimeError("socket connection broken")
                        buffer += chunk
                    return buffer

                return cast(Reply, messageSerializer.read(Reader(readBytes)))
            finally:
                s.close()

    return SocketClient()

*/
