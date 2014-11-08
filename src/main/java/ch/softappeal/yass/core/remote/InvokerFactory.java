package ch.softappeal.yass.core.remote;

@FunctionalInterface public interface InvokerFactory {

    <C> Invoker<C> invoker(ContractId<C> contractId);

}
