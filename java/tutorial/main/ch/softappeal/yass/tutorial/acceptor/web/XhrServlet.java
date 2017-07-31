package ch.softappeal.yass.tutorial.acceptor.web;

import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.transport.SimpleTransportSetup;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.contract.EchoServiceImpl;
import ch.softappeal.yass.tutorial.contract.Logger;
import ch.softappeal.yass.tutorial.contract.UnexpectedExceptionHandler;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.Nullable;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Collectors;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public class XhrServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static void invoke(final SimpleTransportSetup setup, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        try {
            // shows how to use certificates
            final @Nullable X509Certificate[] certificates = (X509Certificate[])httpRequest.getAttribute("javax.servlet.request.X509Certificate");
            if (certificates != null) {
                System.out.println(Arrays.stream(certificates).map(X509Certificate::getSubjectDN).collect(Collectors.toList()));
            }
            setup.server.invocation(false, (Request)setup.messageSerializer.read(Reader.create(httpRequest.getInputStream()))).invoke(
                reply -> setup.messageSerializer.write(reply, Writer.create(httpResponse.getOutputStream()))
            );
            httpResponse.setStatus(201); // should be removed; only needed for TypeScript testcase
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
