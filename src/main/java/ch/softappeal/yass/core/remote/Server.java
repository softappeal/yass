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
 * Container for {@link Service}.
 */
public final class Server extends Common {


  private final class ServerInvoker {

    private final Interceptor serviceInterceptor;
    final MethodMapper methodMapper;
    private final Object implementation;

    ServerInvoker(final Service service) {
      serviceInterceptor = service.interceptor;
      methodMapper = methodMapper(service.contractId.contract);
      implementation = service.implementation;
    }

    Reply invoke(final Interceptor invocationInterceptor, final Method method, @Nullable final Object[] arguments) {
      final Invocation invocation = () -> {
        try {
          return method.invoke(implementation, arguments);
        } catch (final InvocationTargetException e) {
          throw e.getCause();
        }
      };
      final Interceptor interceptor = Interceptors.composite(invocationInterceptor, serviceInterceptor);
      @Nullable final Object value;
      try {
        value = interceptor.invoke(method, arguments, invocation);
      } catch (final Throwable t) {
        return new ExceptionReply(t);
      }
      return new ValueReply(value);
    }

  }


  public static final class ServerInvocation {

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
      return invoker.invoke(interceptor, method, request.arguments);
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


}
