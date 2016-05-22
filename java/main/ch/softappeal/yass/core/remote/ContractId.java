package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.util.Check;

/**
 * Combines a contract with an id.
 * @param <C> the contract type
 */
public final class ContractId<C> {

    public final Class<C> contract;
    public final int id;
    public final MethodMapper methodMapper;

    private ContractId(final Class<C> contract, final int id, final MethodMapper.Factory methodMapperFactory) {
        this.contract = Check.notNull(contract);
        this.id = id;
        methodMapper = methodMapperFactory.create(contract);
    }

    /**
     * It's a good idea to add an interceptor that handles unexpected exceptions
     * (this is especially useful for oneWay methods where these are ignored and NOT passed to the client).
     */
    public Service service(final C implementation, final Interceptor... interceptors) {
        return new Service(this, implementation, Interceptor.composite(interceptors));
    }

    /**
     * @see Server#completer()
     * @see #serviceAsync(Object)
     */
    public Service serviceAsync(final C implementation, final InterceptorAsync<?> interceptor) {
        return new Service(this, implementation, interceptor);
    }

    /**
     * @see #serviceAsync(Object, InterceptorAsync)
     */
    public Service serviceAsync(final C implementation) {
        return serviceAsync(implementation, InterceptorAsync.EMPTY);
    }

    public static <C> ContractId<C> create(final Class<C> contract, final int id, final MethodMapper.Factory methodMapperFactory) {
        return new ContractId<>(contract, id, methodMapperFactory);
    }

}
