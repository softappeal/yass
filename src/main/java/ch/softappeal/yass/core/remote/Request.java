package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

public final class Request extends Message {

    private static final long serialVersionUID = 1L;

    public final int serviceId;

    /**
     * @see MethodMapper.Mapping#id
     */
    public final Object methodId;

    @Nullable public final Object[] arguments;

    public Request(final int serviceId, final Object methodId, @Nullable final Object[] arguments) {
        this.serviceId = serviceId;
        this.methodId = Check.notNull(methodId);
        this.arguments = arguments;
    }

}
