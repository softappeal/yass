package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.remote.Request;
import ch.softappeal.yass.remote.Server;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.shared.EchoServiceImpl;
import ch.softappeal.yass.tutorial.shared.Logger;
import ch.softappeal.yass.tutorial.shared.UnexpectedExceptionHandler;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public class XhrServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static void invoke(final SimpleTransportSetup setup, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        try {
            Optional.ofNullable((X509Certificate[])httpRequest.getAttribute("javax.servlet.request.X509Certificate"))
                .ifPresent(certificates -> System.out.println(Arrays.stream(certificates).map(X509Certificate::getSubjectDN).collect(Collectors.toList())));
            setup.server.invocation(false, (Request)setup.messageSerializer.read(Reader.create(httpRequest.getInputStream()))).invoke(
                reply -> setup.messageSerializer.write(reply, Writer.create(httpResponse.getOutputStream()))
            );
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

    private static final SimpleTransportSetup SETUP = new SimpleTransportSetup(
        Config.MESSAGE_SERIALIZER,
        new Server(
            ACCEPTOR.echoService.service(EchoServiceImpl.INSTANCE, UnexpectedExceptionHandler.INSTANCE, new Logger(null, Logger.Side.SERVER))
        )
    );

    @Override protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        invoke(SETUP, request, response);
    }

}
