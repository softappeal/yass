package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

import java.util.HashSet;
import java.util.Set;

public abstract class Services {

    public final MethodMapper.Factory methodMapperFactory;

    protected Services(final MethodMapper.Factory methodMapperFactory) {
        this.methodMapperFactory = Check.notNull(methodMapperFactory);
    }

    private final Set<Integer> identifiers = new HashSet<>();

    /**
     * @since 36.0.0
     */
    protected final <C> ContractId<C> contractId(final Class<C> contract, final int id) {
        if (!identifiers.add(id)) {
            throw new IllegalArgumentException("service with id " + id + " already added");
        }
        return ContractId.create(contract, id, methodMapperFactory);
    }

    /**
     * @deprecated use {@link #contractId(Class, int)} instead
     */
    @Deprecated
    protected final <C> ContractId<C> create(final Class<C> contract, final int id) {
        return contractId(contract, id);
    }

}
