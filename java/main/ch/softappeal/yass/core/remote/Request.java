package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

import java.util.List;

public final class Request extends Message {

    private static final long serialVersionUID = 1L;

    public final int serviceId;

    /**
     * @see MethodMapper.Mapping#id
     */
    public final int methodId;

    public final List<Object> arguments;

    public Request(final int serviceId, final int methodId, final List<Object> arguments) {
        this.serviceId = serviceId;
        this.methodId = methodId;
        this.arguments = Check.notNull(arguments);
    }

}
