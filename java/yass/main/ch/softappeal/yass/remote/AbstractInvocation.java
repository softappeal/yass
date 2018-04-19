package ch.softappeal.yass.remote;

import ch.softappeal.yass.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractInvocation {

    public final MethodMapper.Mapping methodMapping;
    public final List<Object> arguments;
    public volatile @Nullable Object context;

    AbstractInvocation(final MethodMapper.Mapping methodMapping, final List<Object> arguments) {
        this.methodMapping = Objects.requireNonNull(methodMapping);
        this.arguments = Objects.requireNonNull(arguments);
    }

}
