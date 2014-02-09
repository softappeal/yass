package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Factory for {@link Invoker}.
 */
public abstract class Client extends Common {


  public static final class ClientInvocation {

    public final boolean oneWay;
    private final Interceptor invocationInterceptor;
    private final Object serviceId;
    private final MethodMapper.Mapping methodMapping;
    private final Object[] arguments;

    ClientInvocation(final Interceptor invocationInterceptor, final Object serviceId, final MethodMapper.Mapping methodMapping, final Object[] arguments) {
      this.invocationInterceptor = invocationInterceptor;
      this.serviceId = serviceId;
      this.methodMapping = methodMapping;
      this.arguments = arguments;
      oneWay = methodMapping.oneWay;
    }

    /**
     * @param interceptor prepended to the interceptor chain
     * @see Client#invoke(ClientInvocation)
     */
    @Nullable public Object invoke(final Interceptor interceptor, final Tunnel tunnel) throws Throwable {
      return Interceptors.composite(interceptor, invocationInterceptor).invoke(methodMapping.method, arguments, new Invocation() {
        @Override public Object proceed() throws Throwable {
          final Reply reply = tunnel.invoke(new Request(serviceId, methodMapping.id, arguments));
          return oneWay ? null : reply.process();
        }
      });
    }

  }


  protected Client(final MethodMapper.Factory methodMapperFactory) {
    super(methodMapperFactory);
  }


  /**
   * @see ContractId#invoker(Client)
   */
  final <C> Invoker<C> invoker(final ContractId<C> contractId) {
    final MethodMapper methodMapper = methodMapper(contractId.contract);
    return new Invoker<C>() {
      @Override public C proxy(final Interceptor... interceptors) {
        final Interceptor interceptor = Interceptors.composite(contractId.interceptor, Interceptors.composite(interceptors));
        return contractId.contract.cast(Proxy.newProxyInstance(contractId.contract.getClassLoader(), new Class<?>[] {contractId.contract}, new InvocationHandler() {
          @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
            return Client.this.invoke(new ClientInvocation(interceptor, contractId.id, methodMapper.mapMethod(method), arguments));
          }
        }));
      }
    };
  }


  /**
   * @return {@link ClientInvocation#invoke(Interceptor, Tunnel)}
   */
  @Nullable protected abstract Object invoke(ClientInvocation invocation) throws Throwable;


}
