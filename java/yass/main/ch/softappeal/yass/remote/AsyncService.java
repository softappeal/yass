package ch.softappeal.yass.remote;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

public final class AsyncService extends AbstractService {

    private static final ThreadLocal<Completer> COMPLETER = new ThreadLocal<>();

    /**
     * @return completer for active asynchronous request/reply service invocation
     */
    public static Completer completer() {
        return Optional.ofNullable(COMPLETER.get())
            .orElseThrow(() -> new IllegalStateException("no active asynchronous request/reply service invocation"));
    }

    private final AsyncInterceptor interceptor;

    public <C> AsyncService(final ContractId<C> contractId, final C implementation, final AsyncInterceptor interceptor) {
        super(contractId, implementation);
        this.interceptor = Objects.requireNonNull(interceptor);
    }

    public <C> AsyncService(final ContractId<C> contractId, final C implementation) {
        this(contractId, implementation, DirectAsyncInterceptor.INSTANCE);
    }

    @Override void invoke(final AbstractInvocation invocation, final AbstractService.ReplyWriter replyWriter) throws Exception {
        final var oldCompleter = COMPLETER.get();
        COMPLETER.set(invocation.methodMapping.oneWay ? null : new Completer() {
            @Override public void complete(final @Nullable Object result) {
                try {
                    interceptor.exit(invocation, result);
                    replyWriter.writeReply(new ValueReply(result));
                } catch (final Exception e) {
                    throw Exceptions.wrap(e);
                }
            }
            @Override public void completeExceptionally(final Exception exception) {
                Objects.requireNonNull(exception);
                try {
                    interceptor.exception(invocation, exception);
                    replyWriter.writeReply(new ExceptionReply(exception));
                } catch (final Exception e) {
                    throw Exceptions.wrap(e);
                }
            }
        });
        try {
            interceptor.entry(invocation);
            try {
                invocation.methodMapping.method.invoke(implementation, invocation.arguments.toArray());
            } catch (final InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (final Exception | Error e2) {
                    throw e2;
                } catch (final Throwable t) {
                    throw new Error(t);
                }
            }
        } finally {
            COMPLETER.set(oldCompleter);
        }
    }

}
