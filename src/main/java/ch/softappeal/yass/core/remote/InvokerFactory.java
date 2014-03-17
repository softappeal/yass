package ch.softappeal.yass.core.remote;

public interface InvokerFactory {

  <C> Invoker<C> invoker(ContractId<C> contractId);

}
