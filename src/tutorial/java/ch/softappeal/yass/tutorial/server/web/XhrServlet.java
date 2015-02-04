package ch.softappeal.yass.tutorial.server.web;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.remote.Request;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Server.ServerInvocation;
import ch.softappeal.yass.serialize.Reader;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.serialize.Writer;
import ch.softappeal.yass.tutorial.contract.Config;
import ch.softappeal.yass.tutorial.server.ServerSetup;
import ch.softappeal.yass.util.Exceptions;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XhrServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static void invoke(final Server server, final Serializer serializer, final HttpServletRequest httpRequest, final HttpServletResponse httpResponse) throws Exception {
        final Request request = (Request)serializer.read(Reader.create(httpRequest.getInputStream()));
        final ServerInvocation invocation = server.invocation(request);
        if (invocation.oneWay) {
            throw new IllegalArgumentException("xhr not allowed for oneway method (serviceId " + request.serviceId + ", methodId " + request.methodId + ')');
        }
        serializer.write(
            invocation.invoke(Interceptor.DIRECT), // note: we could add a http session interceptor here if needed
            Writer.create(httpResponse.getOutputStream())
        );
    }

    @Override protected void doPost(final HttpServletRequest request, final HttpServletResponse response) {
        try {
            invoke(ServerSetup.SERVER, Config.MESSAGE_SERIALIZER, request, response);
        } catch (final Exception e) {
            throw Exceptions.wrap(e);
        }
    }

}
