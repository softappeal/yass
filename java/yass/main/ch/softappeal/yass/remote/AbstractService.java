package ch.softappeal.yass.remote;

import java.util.Objects;

public abstract class AbstractService {

    public final ContractId<?> contractId;
    final Object implementation;

    <C> AbstractService(final ContractId<C> contractId, final C implementation) {
        this.contractId = Objects.requireNonNull(contractId);
        this.implementation = Objects.requireNonNull(implementation);
    }

    @FunctionalInterface public interface ReplyWriter {
        void writeReply(Reply reply) throws Exception;
    }

    abstract void invoke(final AbstractInvocation invocation, final ReplyWriter replyWriter) throws Exception;

}