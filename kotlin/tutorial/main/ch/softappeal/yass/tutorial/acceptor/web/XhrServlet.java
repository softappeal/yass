package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.remote.ServerKt;
import ch.softappeal.yass.transport.ServerTransport;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.softappeal.yass.serialize.ReaderKt.reader;
import static ch.softappeal.yass.serialize.WriterKt.writer;
import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public class XhrServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static void invoke(
        final ServerTransport transport, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse
    ) {
        Optional.ofNullable((X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate"))
            .ifPresent(
                certificates -> System.out.println(
                    Arrays.stream(certificates).map(X509Certificate::getSubjectDN).collect(Collectors.toList())
                )
            );
        try {
            transport.invocation(false, transport.read(reader(httpRequest.getInputStream()))).invoke(
                reply -> {
                    try {
                        transport.write(writer(httpResponse.getOutputStream()), reply);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            );
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ServerTransport SETUP = new ServerTransport(
        new Server(
            ServerKt.service(
                ACCEPTOR.echoService,
                EchoServiceImpl.INSTANCE,
                UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER)
            )
        ),
        Config.MESSAGE_SERIALIZER
    );

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        invoke(SETUP, request, response);
    }

}
