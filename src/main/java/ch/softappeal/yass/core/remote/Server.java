package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Server side container for {@link Service}.
 */
public final class Server extends Common {


  private final class ServerInvoker {

    private final ContractId<?> contractId;
    final MethodMapper methodMapper;
    private final Object implementation;
    private final Interceptor serviceInterceptor;

    ServerInvoker(final Service service) {
      contractId = service.contractId;
      methodMapper = methodMapper(contractId.contract);
      implementation = service.implementation;
      serviceInterceptor = service.interceptor;
    }

    Reply invoke(final Interceptor invokerInterceptor, final Request request, final Method method) {
      final Invocation invocation = new Invocation(method, request.arguments) {
        @Override public Object proceed() throws Throwable {
          try {
            return method.invoke(implementation, arguments);
          } catch (final InvocationTargetException e) {
            //noinspection ThrowInsideCatchBlockWhichIgnoresCaughtException
            throw e.getCause();
          }
        }
      };
      invocation.context = request.context;
      final Interceptor interceptor = Interceptors.composite(invokerInterceptor, serviceInterceptor);
      @Nullable final Object value;
      try {
        value = interceptor.invoke(invocation);
      } catch (final Throwable t) {
        return new ExceptionReply(invocation.context, t);
      }
      return new ValueReply(invocation.context, value);
    }

  }


  /**
   * Server side invocation.
   */
  public final class ServerInvocation {

    public final boolean oneWay;
    private final ServerInvoker invoker;
    private final Request request;
    private final Method method;

    ServerInvocation(final ServerInvoker invoker, final Request request) {
      this.invoker = invoker;
      this.request = request;
      final MethodMapper.Mapping methodMapping = invoker.methodMapper.mapId(request.methodId);
      oneWay = methodMapping.oneWay;
      method = methodMapping.method;
    }

    /**
     * @param interceptor prepended to the interceptor chain
     */
    public Reply invoke(final Interceptor interceptor) {
      return invoker.invoke(
        Interceptors.composite(interceptor, Interceptors.threadLocal(ContractId.INSTANCE, invoker.contractId)),
        request,
        method
      );
    }

  }


  private final Map<Object, ServerInvoker> serviceId2invoker;

  public Server(final MethodMapper.Factory methodMapperFactory, final Service... services) {
    super(methodMapperFactory);
    serviceId2invoker = new HashMap<>(services.length);
    for (final Service service : services) {
      if (serviceId2invoker.put(service.contractId.id, new ServerInvoker(service)) != null) {
        throw new IllegalArgumentException("serviceId '" + service.contractId.id + "' already added");
      }
    }
  }

  public ServerInvocation invocation(final Request request) {
    final ServerInvoker invoker = serviceId2invoker.get(request.serviceId);
    if (invoker == null) {
      throw new RuntimeException("no serviceId '" + request.serviceId + "' found (methodId '" + request.methodId + "')");
    }
    return new ServerInvocation(invoker, request);
  }


  /**
   * A {@link Client} for this server.
   */
  public final Client client = new Client(methodMapperFactory) {
    @Override public Object invoke(final ClientInvocation invocation) throws Throwable {
      return invocation.invoke(Interceptors.DIRECT, new Tunnel() {
        @Override public Reply invoke(final Request request) {
          return invocation(request).invoke(Interceptors.DIRECT);
        }
      });
    }
  };


}
