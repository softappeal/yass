package ch.softappeal.yass.remote;

import ch.softappeal.yass.Interceptor;

public final class Service extends AbstractService {

    private final Interceptor interceptor;

    public <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
        super(contractId, implementation);
        this.interceptor = Interceptor.composite(interceptors);
    }

    @Override void invoke(final AbstractInvocation invocation, final AbstractService.ReplyWriter replyWriter) throws Exception {
        Reply reply;
        try {
            reply = new ValueReply(Interceptor.invoke(interceptor, invocation.methodMapping.method, implementation, invocation.arguments.toArray()));
        } catch (final Exception e) {
            if (invocation.methodMapping.oneWay) {
                throw e;
            }
            reply = new ExceptionReply(e);
        }
        replyWriter.writeReply(reply);
    }

}
