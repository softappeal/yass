package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Client side factory for {@link Invoker}.
 */
public abstract class Client extends Common {


  /**
   * Client side invocation.
   */
  public static final class ClientInvocation {

    public final boolean oneWay;
    private final Interceptor invocationInterceptor;
    private final Object serviceId;
    private final MethodMapper.Mapping mapping;
    private final Object[] arguments;

    ClientInvocation(final Interceptor invocationInterceptor, final Object serviceId, final MethodMapper.Mapping mapping, final Object[] arguments) {
      this.invocationInterceptor = invocationInterceptor;
      this.serviceId = serviceId;
      this.mapping = mapping;
      //noinspection AssignmentToCollectionOrArrayFieldFromParameter
      this.arguments = arguments;
      oneWay = mapping.oneWay;
    }

    /**
     * @param interceptor prepended to the interceptor chain
     * @see Client#invoke(ClientInvocation)
     */
    @Nullable public Object invoke(final Interceptor interceptor, final Tunnel tunnel) throws Throwable {
      return Interceptors.composite(interceptor, invocationInterceptor).invoke(new Invocation(mapping.method, arguments) {
        @Override public Object proceed() throws Throwable {
          final Reply reply = tunnel.invoke(new Request(context, serviceId, mapping.methodId, arguments));
          if (oneWay) {
            context = null;
            return null;
          }
          context = reply.context;
          return reply.process();
        }
      });
    }

  }


  protected Client(final MethodMapper.Factory methodMapperFactory) {
    super(methodMapperFactory);
  }


  final <C> Invoker<C> invoker(final ContractId<C> contractId) {
    final MethodMapper mapper = methodMapper(contractId.contract);
    final Interceptor invokerInterceptor = Interceptors.threadLocal(ContractId.INSTANCE, contractId);
    return new Invoker<C>() {
      @Override public C proxy(final Interceptor... interceptors) {
        final Interceptor interceptor = Interceptors.composite(invokerInterceptor, Interceptors.composite(interceptors));
        return contractId.contract.cast(Proxy.newProxyInstance(contractId.contract.getClassLoader(), new Class<?>[] {contractId.contract}, new InvocationHandler() {
          @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
            return Client.this.invoke(new ClientInvocation(interceptor, contractId.id, mapper.mapMethod(method), arguments));
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
