package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.transport.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.shared.*;

import javax.servlet.http.*;
import java.io.*;
import java.security.cert.*;
import java.util.*;
import java.util.stream.*;

import static ch.softappeal.yass.serialize.ReaderKt.*;
import static ch.softappeal.yass.serialize.WriterKt.*;
import static ch.softappeal.yass.tutorial.contract.Config.*;

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
