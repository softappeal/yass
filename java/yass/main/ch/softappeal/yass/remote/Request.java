package ch.softappeal.yass.remote;

import java.util.List;
import java.util.Objects;

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
        this.arguments = Objects.requireNonNull(arguments);
    }

}
