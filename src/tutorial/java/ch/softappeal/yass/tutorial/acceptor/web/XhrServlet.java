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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static ch.softappeal.yass.tutorial.contract.Config.ACCEPTOR;

public class XhrServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static void invoke(final SimpleTransportSetup setup, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) {
        try {
            setup.messageSerializer.write(
                setup.server.invocation(
                    (Request)setup.messageSerializer.read(Reader.create(httpRequest.getInputStream()))
                ).invoke(),
                Writer.create(httpResponse.getOutputStream())
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
