public static <T> T notNull(@Nullable final T value) throws NullPointerException {
  if (value == null) {
    throw new NullPointerException();
  }
  return value;
}

public static int hasTag(final AnnotatedElement element) throws IllegalArgumentException {
  final Tag annotation = element.getAnnotation(Tag.class);
  if (annotation == null) {
    throw new IllegalArgumentException("missing tag for '" + element + '\'');
  }
  return annotation.value();
}

public interface Interceptor {
  @Nullable Object invoke(Method method, @Nullable Object[] arguments, Invocation invocation) throws Throwable;
}

public interface Invocation {
  @Nullable Object proceed() throws Throwable;
}

public final class Interceptors {

  public static <C> C proxy(final Class<C> contract, final C implementation, final Interceptor... interceptors) {
    Check.notNull(implementation);
    final Interceptor interceptor = composite(interceptors);
    return contract.cast(Proxy.newProxyInstance(contract.getClassLoader(), new Class<?>[] {contract}, new InvocationHandler() {
      @Override public Object invoke(final Object proxy, final Method method, final Object[] arguments) throws Throwable {
        return interceptor.invoke(method, arguments, new Invocation() {
          @Override public Object proceed() throws Throwable {
            try {
              return method.invoke(implementation, arguments);
            } catch (final InvocationTargetException e) {
              throw e.getCause();
            }
          }
        });
      }
    }));
  }

  public static final Interceptor DIRECT = new Interceptor() {
    @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
      return invocation.proceed();
    }
  };

  public static Interceptor composite(final Interceptor interceptor1, final Interceptor interceptor2) {
    Check.notNull(interceptor1);
    Check.notNull(interceptor2);
    if (interceptor1 == DIRECT) {
      return interceptor2;
    }
    if (interceptor2 == DIRECT) {
      return interceptor1;
    }
    return new Interceptor() {
      @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
        return interceptor1.invoke(method, arguments, new Invocation() {
          @Override @Nullable public Object proceed() throws Throwable {
            return interceptor2.invoke(method, arguments, invocation);
          }
        });
      }
    };
  }

  public static Interceptor composite(final Interceptor... interceptors) {
    Interceptor compositeInterceptor = DIRECT;
    for (final Interceptor interceptor : interceptors) {
      compositeInterceptor = composite(compositeInterceptor, interceptor);
    }
    return compositeInterceptor;
  }

  public static <T> Interceptor threadLocal(final ThreadLocal<T> threadLocal, final T value) {
    Check.notNull(threadLocal);
    Check.notNull(value);
    return new Interceptor() {
      @Override @Nullable public Object invoke(final Method method, @Nullable final Object[] arguments, final Invocation invocation) throws Throwable {
        @Nullable final T oldValue = threadLocal.get();
        threadLocal.set(value);
        try {
          return invocation.proceed();
        } finally {
          threadLocal.set(oldValue);
        }
      }
    };
  }

  public static boolean hasInvocation(final ThreadLocal<?> threadLocal) {
    return threadLocal.get() != null;
  }

  public static <T> T getInvocation(final ThreadLocal<T> threadLocal) throws IllegalStateException {
    @Nullable final T value = threadLocal.get();
    if (value == null) {
      throw new IllegalStateException("no active invocation");
    }
    return value;
  }

}

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

  @Nullable protected abstract Object invoke(ClientInvocation invocation) throws Throwable;

}

public abstract class Common {

  Common(final MethodMapper.Factory methodMapperFactory) {
    this.methodMapperFactory = Check.notNull(methodMapperFactory);
  }

  public final MethodMapper.Factory methodMapperFactory;

  final MethodMapper methodMapper(final Class<?> contract) {
    return methodMapperFactory.create(contract);
  }

}

public final class ContractId<C> {

  private static final ThreadLocal<ContractId<?>> INSTANCE = new ThreadLocal<>();

  public static boolean hasInvocation() {
    return Interceptors.hasInvocation(INSTANCE);
  }

  public static ContractId<?> get() {
    return Interceptors.getInvocation(INSTANCE);
  }

  public final Class<C> contract;
  public final Object id;
  final Interceptor interceptor = Interceptors.threadLocal(INSTANCE, this);

  private ContractId(final Class<C> contract, final Object id) {
    this.contract = Check.notNull(contract);
    this.id = Check.notNull(id);
  }

  public Service service(final C implementation, final Interceptor... interceptors) {
    return new Service(this, implementation, interceptors);
  }

  public Invoker<C> invoker(final Client client) {
    return client.invoker(this);
  }

  public static <C> ContractId<C> create(final Class<C> contract, final Object id) {
    return new ContractId<>(contract, id);
  }

}

public final class ExceptionReply extends Reply {

  private static final long serialVersionUID = 1L;

  public final Throwable throwable;

  public ExceptionReply(final Throwable throwable) {
    this.throwable = Check.notNull(throwable);
  }

  @Override Object process() throws Throwable {
    throw throwable;
  }

}

public interface Invoker<C> {
  C proxy(Interceptor... interceptors);
}

public abstract class Message;

public interface MethodMapper {

  final class Mapping {
    public final Method method;
    public final Object id;
    public final boolean oneWay;
    public Mapping(final Method method, final Object id, final boolean oneWay) {
      this.method = Check.notNull(method);
      this.id = Check.notNull(id);
      this.oneWay = oneWay;
      if (oneWay) {
        if (method.getReturnType() != Void.TYPE) {
          throw new IllegalArgumentException("oneway method '" + method + "' must return 'void'");
        }
        if (method.getExceptionTypes().length != 0) {
          throw new IllegalArgumentException("oneway method '" + method + "' must not throw exceptions");
        }
      }
    }
  }

  Mapping mapId(Object id);

  Mapping mapMethod(Method method);

  interface Factory {
    MethodMapper create(Class<?> contract);
  }

}

public abstract class Reply extends Message {
  @Nullable abstract Object process() throws Throwable;
}

public final class Request extends Message {
  public final Object serviceId;
  public final Object methodId;
  @Nullable public final Object[] arguments;
  public Request(final Object serviceId, final Object methodId, @Nullable final Object[] arguments) {
    this.serviceId = Check.notNull(serviceId);
    this.methodId = Check.notNull(methodId);
    this.arguments = arguments;
  }
}

public final class Server extends Common {

  private final class ServerInvoker {

    private final Interceptor invokerInterceptor;
    final MethodMapper methodMapper;
    private final Object implementation;

    ServerInvoker(final Service service) {
      invokerInterceptor = Interceptors.composite(service.contractId.interceptor, service.interceptor);
      methodMapper = methodMapper(service.contractId.contract);
      implementation = service.implementation;
    }

    Reply invoke(final Interceptor invocationInterceptor, final Method method, @Nullable final Object[] arguments) {
      final Invocation invocation = new Invocation() {
        @Override public Object proceed() throws Throwable {
          try {
            return method.invoke(implementation, arguments);
          } catch (final InvocationTargetException e) {
            throw e.getCause();
          }
        }
      };
      final Interceptor interceptor = Interceptors.composite(invocationInterceptor, invokerInterceptor);
      @Nullable final Object value;
      try {
        value = interceptor.invoke(method, arguments, invocation);
      } catch (final Throwable t) {
        return new ExceptionReply(t);
      }
      return new ValueReply(value);
    }

  }

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

public final class Service {

  final ContractId<?> contractId;
  final Object implementation;
  final Interceptor interceptor;

  <C> Service(final ContractId<C> contractId, final C implementation, final Interceptor... interceptors) {
    this.contractId = Check.notNull(contractId);
    this.implementation = Check.notNull(implementation);
    interceptor = Interceptors.composite(interceptors);
  }

}

public final class TaggedMethodMapper implements MethodMapper {

  private final Map<Integer, Mapping> id2mapping;

  private TaggedMethodMapper(final Class<?> contract) {
    final Method[] methods = contract.getMethods();
    id2mapping = new HashMap<>(methods.length);
    for (final Method method : methods) {
      final int id = Check.hasTag(method);
      final Mapping oldMapping = id2mapping.put(id, new Mapping(method, id, method.getAnnotation(OneWay.class) != null));
      if (oldMapping != null) {
        throw new IllegalArgumentException("tag " + id + " used for methods '" + method + "' and '" + oldMapping.method + '\'');
      }
    }
  }

  @Override public Mapping mapId(final Object id) {
    return id2mapping.get(id);
  }

  @Override public Mapping mapMethod(final Method method) {
    return id2mapping.get(method.getAnnotation(Tag.class).value());
  }

  public static final Factory FACTORY = new Factory() {
    @Override public MethodMapper create(final Class<?> contract) {
      return new TaggedMethodMapper(contract);
    }
  };

}

public interface Tunnel {
  @Nullable Reply invoke(Request request) throws Exception;
}

public final class ValueReply extends Reply {
  @Nullable public final Object value;
  public ValueReply(@Nullable final Object value) {
    this.value = value;
  }
  @Override @Nullable Object process() {
    return value;
  }
}

public abstract class Connection {

  protected abstract void closed() throws Exception;

  protected abstract void write(Packet packet) throws Exception;

  protected static void close(final Session session, final Throwable throwable) {
    session.close(throwable);
  }

  protected static void received(final Session session, final Packet packet) {
    session.received(packet);
  }

  protected static boolean open(final Session session) {
    return session.open();
  }

}

public final class Packet implements Serializable {

  private static final long serialVersionUID = 1L;

  private final int requestNumber;
  @Nullable private final Message message;

  public boolean isEnd() {
    return requestNumber == END_REQUEST_NUMBER;
  }

  private void checkNotEnd() {
    if (isEnd()) {
      throw new IllegalStateException("not allowed if isEnd");
    }
  }

  public int requestNumber() {
    checkNotEnd();
    return requestNumber;
  }

  public Message message() {
    checkNotEnd();
    return message;
  }

  public Packet(final int requestNumber, final Message message) {
    if (requestNumber == END_REQUEST_NUMBER) {
      throw new IllegalArgumentException("use END");
    }
    this.requestNumber = requestNumber;
    this.message = Check.notNull(message);
  }

  private Packet() {
    requestNumber = END_REQUEST_NUMBER;
    message = null;
  }

  public static boolean isEnd(final int requestNumber) {
    return requestNumber == END_REQUEST_NUMBER;
  }

  public static final Packet END = new Packet();

  public static final int END_REQUEST_NUMBER = 0;

}

public final class RequestInterruptedException extends RuntimeException

public abstract class Session extends Client implements AutoCloseable {

  private static final ThreadLocal<Session> INSTANCE = new ThreadLocal<>();

  public static boolean hasInvocation() {
    return Interceptors.hasInvocation(INSTANCE);
  }

  public static Session get() {
    return Interceptors.getInvocation(INSTANCE);
  }

  private final SessionSetup setup;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  public final Connection connection;
  private final Interceptor sessionInterceptor = Interceptors.threadLocal(INSTANCE, this);
  private boolean opened = false;

  protected Session(final SessionSetup setup, final Connection connection) {
    super(setup.server.methodMapperFactory);
    this.setup = setup;
    this.connection = Check.notNull(connection);
  }

  protected void opened() throws Exception {
  }

  protected abstract void closed(@Nullable Throwable throwable);

  final boolean open() {
    opened = true;
    try {
      setup.requestExecutor.execute(new Runnable() {
        @Override public void run() {
          try {
            opened();
          } catch (final Exception e) {
            close(e);
          }
        }
      });
    } catch (final Exception e) {
      close(e);
      return false;
    }
    return true;
  }

  private void close(final boolean sendEnd, @Nullable final Throwable throwable) {
    try {
      if (closed.getAndSet(true)) {
        return;
      }
      try {
        closed(throwable);
        if (sendEnd) {
          connection.write(Packet.END);
        }
      } catch (final Exception e) {
        try {
          connection.closed();
        } catch (final Exception e2) {
          e.addSuppressed(e2);
        }
        throw e;
      }
      connection.closed();
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  private void write(final int requestNumber, final Message message) {
    if (closed.get()) {
      throw new SessionClosedException();
    }
    try {
      connection.write(new Packet(requestNumber, message));
    } catch (final Exception e) {
      close(e);
    }
  }

  private final Map<Integer, BlockingQueue<Reply>> requestNumber2replyQueue = Collections.synchronizedMap(new HashMap<Integer, BlockingQueue<Reply>>(16));

  private void writeReply(final int requestNumber, final Reply reply) throws InterruptedException {
    @Nullable final BlockingQueue<Reply> replyQueue = requestNumber2replyQueue.remove(requestNumber);
    if (replyQueue != null) { // needed because request can be interrupted, see below
      replyQueue.put(reply);
    }
  }

  private Reply requestInterrupted(final int requestNumber) {
    requestNumber2replyQueue.remove(requestNumber);
    return new ExceptionReply(new RequestInterruptedException());
  }

  private Reply writeRequestAndReadReply(final int requestNumber, final Request request) {
    final BlockingQueue<Reply> replyQueue = new ArrayBlockingQueue<>(1, false); // we use unfair for speed
    if (requestNumber2replyQueue.put(requestNumber, replyQueue) != null) {
      throw new RuntimeException("already waiting for requestNumber " + requestNumber);
    }
    write(requestNumber, request); // note: must be after line above to prevent race conditions with method above
    while (true) {
      if (Thread.interrupted()) {
        return requestInterrupted(requestNumber);
      }
      try {
        final Reply reply = replyQueue.poll(100L, TimeUnit.MILLISECONDS);
        if (reply != null) {
          return reply;
        } else if (closed.get()) {
          throw new SessionClosedException();
        }
      } catch (final InterruptedException ignored) {
        return requestInterrupted(requestNumber);
      }
    }
  }

  private void serverInvoke(final int requestNumber, final Request request) {
    setup.requestExecutor.execute(new Runnable() {
      @Override public void run() {
        try {
          final ServerInvocation invocation = setup.server.invocation(request);
          final Reply reply = invocation.invoke(sessionInterceptor);
          if (!invocation.oneWay) {
            write(requestNumber, reply);
          }
        } catch (final Exception e) {
          close(e);
        }
      }
    });
  }

  final void received(final Packet packet) {
    try {
      if (packet.isEnd()) {
        close(null);
        return;
      }
      final Message message = packet.message();
      if (message instanceof Request) {
        serverInvoke(packet.requestNumber(), (Request)message);
      } else { // Reply
        writeReply(packet.requestNumber(), (Reply)message);
      }
    } catch (final Exception e) {
      close(e);
    }
  }

  final void close(final Throwable throwable) {
    close(false, throwable);
  }

  private final AtomicInteger nextRequestNumber = new AtomicInteger(Packet.END_REQUEST_NUMBER);

  @Override protected final Object invoke(final ClientInvocation invocation) throws Throwable {
    if (!opened) {
      throw new IllegalStateException("session is not yet opened");
    }
    return invocation.invoke(sessionInterceptor, new Tunnel() {
      @Override public Reply invoke(final Request request) {
        int requestNumber;
        do { // we can't use END_REQUEST_NUMBER as regular requestNumber
          requestNumber = nextRequestNumber.incrementAndGet();
        } while (requestNumber == Packet.END_REQUEST_NUMBER);
        if (invocation.oneWay) {
          write(requestNumber, request);
          return null;
        }
        return writeRequestAndReadReply(requestNumber, request);
      }
    });
  }

  @Override public final void close() {
    close(true, null);
  }

}

public final class SessionClosedException extends RuntimeException

public interface SessionFactory {
  Session create(SessionSetup setup, Connection connection) throws Exception;
}

public final class SessionSetup {
  final Server server;
  final Executor requestExecutor;
  private final SessionFactory sessionFactory;
  public SessionSetup(final Server server, final Executor requestExecutor, final SessionFactory sessionFactory) {
    this.server = Check.notNull(server);
    this.requestExecutor = Check.notNull(requestExecutor);
    this.sessionFactory = Check.notNull(sessionFactory);
  }
  public Session createSession(final Connection connection) throws Exception {
    return sessionFactory.create(this, connection);
  }
}

public abstract class Reader {

  public abstract byte readByte() throws Exception;

  public abstract void readBytes(byte[] buffer, int offset, int length) throws Exception;

  public final void readBytes(final byte[] buffer) throws Exception {
    readBytes(buffer, 0, buffer.length);
  }

  public final int readInt() throws Exception {
    return
      ((readByte() & 0b1111_1111) << 24) |
      ((readByte() & 0b1111_1111) << 16) |
      ((readByte() & 0b1111_1111) << 8) |
      ((readByte() & 0b1111_1111) << 0);
  }

  public final int readVarInt() throws Exception {
    byte b = readByte();
    if (b >= 0) {
      return b;
    }
    int value = b & 0b0111_1111;
    if ((b = readByte()) >= 0) {
      value |= b << 7;
    } else {
      value |= (b & 0b0111_1111) << 7;
      if ((b = readByte()) >= 0) {
        value |= b << 14;
      } else {
        value |= (b & 0b0111_1111) << 14;
        if ((b = readByte()) >= 0) {
          value |= b << 21;
        } else {
          value |= (b & 0b0111_1111) << 21;
          value |= (b = readByte()) << 28;
          if (b < 0) {
            throw new RuntimeException("malformed input");
          }
        }
      }
    }
    return value;
  }

  public final int readZigZagInt() throws Exception {
    final int value = readVarInt();
    return (value >>> 1) ^ -(value & 1);
  }

  public final InputStream stream() {
    return new InputStream() {
      @Override public int read() {
        try {
          return readByte() & 0b1111_1111;
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
      @Override public int read(final byte[] b, final int off, final int len) {
        try {
          readBytes(b, off, len);
          return len;
        } catch (final Exception e) {
          throw Exceptions.wrap(e);
        }
      }
    };
  }

}

public interface Reflector {

  interface Accessor {
    @Nullable Object get(Object object) throws Exception;
    void set(Object object, @Nullable Object value) throws Exception;
  }

  Object newInstance() throws Exception;

  Accessor accessor(Field field);

  interface Factory {
    Reflector create(Class<?> type) throws Exception;
  }


}

public interface Serializer {
  Object read(Reader reader) throws Exception;
  void write(Object value, Writer writer) throws Exception;
}

public final class SlowReflector implements Reflector {

  public static final Factory FACTORY = new Factory() {
    @Override public Reflector create(final Class<?> type) throws NoSuchMethodException {
      return new SlowReflector(type);
    }
  };

  private SlowReflector(final Class<?> type) throws NoSuchMethodException {
    constructor = type.getDeclaredConstructor();
    if (!Modifier.isPublic(constructor.getModifiers())) {
      constructor.setAccessible(true);
    }
  }

  private final Constructor<?> constructor;

  @Override public Object newInstance() throws Exception {
    return constructor.newInstance();
  }

  @Override public Accessor accessor(final Field field) {
    final int modifiers = field.getModifiers();
    if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) {
      field.setAccessible(true);
    }
    return new Accessor() {
      @Override public Object get(final Object object) throws IllegalAccessException {
        return field.get(object);
      }
      @Override public void set(final Object object, @Nullable final Object value) throws IllegalAccessException {
        field.set(object, value);
      }
    };
  }

}

public final class Utf8 {
  public static byte[] bytes(final String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
  public static String string(final byte[] value) {
    return new String(value, StandardCharsets.UTF_8);
  }
}

public abstract class Writer {

  public abstract void writeByte(byte value) throws Exception;

  public abstract void writeBytes(byte[] buffer, int offset, int length) throws Exception;

  public final void writeBytes(final byte[] buffer) throws Exception {
    writeBytes(buffer, 0, buffer.length);
  }

  public final void writeInt(final int value) throws Exception {
    writeByte((byte)(value >> 24));
    writeByte((byte)(value >> 16));
    writeByte((byte)(value >> 8));
    writeByte((byte)(value >> 0));
  }
  public final void writeVarInt(int value) throws Exception {
    while (true) {
      if ((value & ~0b0111_1111) == 0) {
        writeByte((byte)value);
        return;
      }
      writeByte((byte)((value & 0b0111_1111) | 0b1000_0000));
      value >>>= 7;
    }
  }

  public final void writeZigZagInt(final int value) throws Exception {
    writeVarInt((value << 1) ^ (value >> 31));
  }

}

public abstract class AbstractFastSerializer implements Serializer {

  private final Reflector.Factory reflectorFactory;

  private Reflector reflector(final Class<?> type) {
    try {
      return reflectorFactory.create(type);
    } catch (final Exception e) {
      throw Exceptions.wrap(e);
    }
  }

  private final Map<Class<?>, TypeDesc> class2typeDesc = new HashMap<>(64);
  private final Map<Integer, TypeHandler> id2typeHandler = new HashMap<>(64);

  private void addType(final TypeDesc typeDesc) {
    if (class2typeDesc.put(typeDesc.handler.type, typeDesc) != null) {
      throw new IllegalArgumentException("type '" + typeDesc.handler.type.getCanonicalName() + "' already added");
    }
    final TypeHandler oldTypeHandler = id2typeHandler.put(typeDesc.id, typeDesc.handler);
    if (oldTypeHandler != null) {
      throw new IllegalArgumentException(
        "type id " + typeDesc.id + " used for '" + typeDesc.handler.type.getCanonicalName() + "' and '" +
        oldTypeHandler.type.getCanonicalName() + '\''
      );
    }
  }

  protected final void addEnum(final int id, final Class<?> type) {
    if (!type.isEnum()) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is not an enumeration");
    }
    final Class<Enum<?>> enumeration = (Class)type;
    final Enum<?>[] constants = enumeration.getEnumConstants();
    addType(new TypeDesc(id, new BaseTypeHandler<Enum<?>>(enumeration) {
      @Override public Enum read(final Reader reader) throws Exception {
        return constants[reader.readVarInt()];
      }
      @Override public void write(final Enum value, final Writer writer) throws Exception {
        writer.writeVarInt(value.ordinal());
      }
    }));
  }

  protected static void checkClass(final Class<?> type) {
    if (Modifier.isAbstract(type.getModifiers())) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is abstract");
    } else if (type.isEnum()) {
      throw new IllegalArgumentException("type '" + type.getCanonicalName() + "' is an enumeration");
    }
  }

  protected final void addClass(final int id, final Class<?> type, final boolean referenceable, final Map<Integer, Field> id2field) {
    final Reflector reflector = reflector(type);
    final Map<Integer, FieldHandler> id2fieldHandler = new HashMap<>(id2field.size());
    for (final Map.Entry<Integer, Field> entry : id2field.entrySet()) {
      final Field field = entry.getValue();
      id2fieldHandler.put(entry.getKey(), new FieldHandler(field, reflector.accessor(field)));
    }
    addType(new TypeDesc(id, new ClassTypeHandler(type, reflector, referenceable, id2fieldHandler)));
  }

  protected final void addBaseType(final TypeDesc typeDesc) {
    if (typeDesc.handler.type.isEnum()) {
      throw new IllegalArgumentException("base type '" + typeDesc.handler.type.getCanonicalName() + "' is an enumeration");
    }
    addType(typeDesc);
  }

  protected AbstractFastSerializer(final Reflector.Factory reflectorFactory) {
    this.reflectorFactory = Check.notNull(reflectorFactory);
    addType(TypeDesc.NULL);
    addType(TypeDesc.REFERENCE);
    addType(TypeDesc.LIST);
  }

  protected final void fixupFields() {
    for (final TypeDesc typeDesc : class2typeDesc.values()) {
      if (typeDesc.handler instanceof ClassTypeHandler) {
        ((ClassTypeHandler)typeDesc.handler).fixupFields(class2typeDesc);
      }
    }
  }

  @Override public final Object read(final Reader reader) throws Exception {
    return new Input(reader, id2typeHandler).read();
  }

  @Override public final void write(final Object value, final Writer writer) throws Exception {
    new Output(writer, class2typeDesc).write(value);
  }

  public final SortedMap<Integer, TypeHandler> id2typeHandler() {
    return new TreeMap<>(id2typeHandler);
  }

  public static List<Field> ownFields(final Class<?> type) {
    final List<Field> fields = new ArrayList<>(16);
    for (final Field field : type.getDeclaredFields()) {
      final int modifiers = field.getModifiers();
      if (!(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers))) {
        fields.add(field);
      }
    }
    return fields;
  }

  public static List<Field> allFields(Class<?> type) {
    final List<Field> fields = new ArrayList<>(16);
    while ((type != null) && (type != Throwable.class)) {
      fields.addAll(ownFields(type));
      type = type.getSuperclass();
    }
    return fields;
  }

}

public abstract class BaseTypeHandler<V> extends TypeHandler {

  protected BaseTypeHandler(final Class<V> type) {
    super(type);
  }

  public abstract V read(Reader reader) throws Exception;

  @Override final Object read(final Input input) throws Exception {
    return read(input.reader);
  }

  public abstract void write(V value, Writer writer) throws Exception;

  @Override final void write(final Object value, final Output output) throws Exception {
    write((V)value, output.writer);
  }

}

public final class BaseTypeHandlers {

  public static final BaseTypeHandler<Boolean> BOOLEAN = new BaseTypeHandler<Boolean>(Boolean.class) {
    @Override public Boolean read(final Reader reader) throws Exception {
      return reader.readByte() != 0;
    }
    @Override public void write(final Boolean value, final Writer writer) throws Exception {
      writer.writeByte(value ? (byte)1 : (byte)0);
    }
  };

  public static final BaseTypeHandler<Integer> INTEGER = new BaseTypeHandler<Integer>(Integer.class) {
    @Override public Integer read(final Reader reader) throws Exception {
      return reader.readZigZagInt();
    }
    @Override public void write(final Integer value, final Writer writer) throws Exception {
      writer.writeZigZagInt(value);
    }
  };

  public static final BaseTypeHandler<byte[]> BYTE_ARRAY = new BaseTypeHandler<byte[]>(byte[].class) {
    @Override public byte[] read(final Reader reader) throws Exception {
      final int length = reader.readVarInt();
      byte[] value = new byte[Math.min(length, 1024)];
      for (int i = 0; i < length; i++) {
        if (i >= value.length) {
          value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
        }
        value[i] = reader.readByte();
      }
      return value;
    }
    @Override public void write(final byte[] value, final Writer writer) throws Exception {
      writer.writeVarInt(value.length);
      writer.writeBytes(value);
    }
  };

  public static final BaseTypeHandler<String> STRING = new BaseTypeHandler<String>(String.class) {
    @Override public String read(final Reader reader) throws Exception {
      return Utf8.string(BYTE_ARRAY.read(reader));
    }
    @Override public void write(final String value, final Writer writer) throws Exception {
      BYTE_ARRAY.write(Utf8.bytes(value), writer);
    }
  };

}

public final class ClassTypeHandler extends TypeHandler {

  public static final class FieldDesc {
    public final int id;
    public final FieldHandler handler;
    private FieldDesc(final int id, final FieldHandler handler) {
      this.id = id;
      this.handler = handler;
    }
  }

  private final Reflector reflector;
  public final boolean referenceable;
  private final Map<Integer, FieldHandler> id2fieldHandler;

  private final FieldDesc[] fieldDescs;
  public FieldDesc[] fieldDescs() {
    return fieldDescs.clone();
  }

  ClassTypeHandler(final Class<?> type, final Reflector reflector, final boolean referenceable, final Map<Integer, FieldHandler> id2fieldHandler) {
    super(type);
    this.reflector = Check.notNull(reflector);
    this.referenceable = referenceable;
    fieldDescs = new FieldDesc[id2fieldHandler.size()];
    int fd = 0;
    for (final Map.Entry<Integer, FieldHandler> entry : id2fieldHandler.entrySet()) {
      final FieldDesc fieldDesc = new FieldDesc(entry.getKey(), entry.getValue());
      if (fieldDesc.id < FieldHandler.FIRST_ID) {
        throw new IllegalArgumentException("id " + fieldDesc.id + " for field '" + fieldDesc.handler.field + "' must be >= " + FieldHandler.FIRST_ID);
      }
      fieldDescs[fd++] = fieldDesc;
    }
    this.id2fieldHandler = new HashMap<>(id2fieldHandler);
    Arrays.sort(fieldDescs, new Comparator<FieldDesc>() {
      @Override public int compare(final FieldDesc fieldDesc1, final FieldDesc fieldDesc2) {
        return ((Integer)fieldDesc1.id).compareTo(fieldDesc2.id);
      }
    });
  }

  void fixupFields(final Map<Class<?>, TypeDesc> class2typeDesc) {
    for (final FieldHandler fieldHandler : id2fieldHandler.values()) {
      fieldHandler.fixup(class2typeDesc);
    }
  }

  @Override Object read(final Input input) throws Exception {
    final Object object = reflector.newInstance();
    if (referenceable) {
      if (input.referenceableObjects == null) {
        input.referenceableObjects = new ArrayList<>(16);
      }
      input.referenceableObjects.add(object);
    }
    while (true) {
      final int id = input.reader.readVarInt();
      if (id == FieldHandler.END_ID) {
        return object;
      }
      id2fieldHandler.get(id).read(object, input);
    }
  }

  @Override void write(final int id, final Object value, final Output output) throws Exception {
    if (referenceable) {
      if (output.object2reference == null) {
        output.object2reference = new IdentityHashMap<>(16);
      }
      final Map<Object, Integer> object2reference = output.object2reference;
      final Integer reference = object2reference.get(value);
      if (reference != null) {
        TypeDesc.REFERENCE.write(reference, output);
        return;
      }
      object2reference.put(value, object2reference.size());
    }
    super.write(id, value, output);
  }

  @Override void write(final Object value, final Output output) throws Exception {
    for (final FieldDesc fieldDesc : fieldDescs) {
      fieldDesc.handler.write(fieldDesc.id, value, output);
    }
    output.writer.writeVarInt(FieldHandler.END_ID);
  }

}

public final class FieldHandler {

  static final int END_ID = 0;
  public static final int FIRST_ID = END_ID + 1;

  public final Field field;
  private final Reflector.Accessor accessor;

  @Nullable private TypeHandler typeHandler;
  @Nullable public TypeHandler typeHandler() {
    return typeHandler;
  }

  FieldHandler(final Field field, final Reflector.Accessor accessor) {
    this.field = Check.notNull(field);
    this.accessor = Check.notNull(accessor);
  }

  void fixup(final Map<Class<?>, TypeDesc> class2typeDesc) {
    final TypeDesc typeDesc = class2typeDesc.get(
      primitiveWrapperType(field.getType()) // note: prevents that primitive types are written with type id
    );
    typeHandler = (typeDesc == null) ? null : typeDesc.handler;
    if (typeHandler instanceof ClassTypeHandler) {
      typeHandler = null;
    }
  }

  void read(final Object object, final Input input) throws Exception {
    accessor.set(object, (typeHandler == null) ? input.read() : typeHandler.read(input));
  }

  void write(final int id, final Object object, final Output output) throws Exception {
    final Object value = accessor.get(object);
    if (value != null) {
      output.writer.writeVarInt(id);
      if (typeHandler == null) {
        output.write(value);
      } else {
        typeHandler.write(value, output);
      }
    }
  }

  private static Class<?> primitiveWrapperType(final Class<?> type) {
    if (type == boolean.class) {
      return Boolean.class;
    }
    if (type == byte.class) {
      return Byte.class;
    }
    if (type == short.class) {
      return Short.class;
    }
    if (type == int.class) {
      return Integer.class;
    }
    if (type == long.class) {
      return Long.class;
    }
    if (type == char.class) {
      return Character.class;
    }
    if (type == float.class) {
      return Float.class;
    }
    if (type == double.class) {
      return Double.class;
    }
    return type;
  }

}

final class Input {

  final Reader reader;
  private final Map<Integer, TypeHandler> id2typeHandler;
  List<Object> referenceableObjects;

  Input(final Reader reader, final Map<Integer, TypeHandler> id2typeHandler) {
    this.reader = reader;
    this.id2typeHandler = id2typeHandler;
  }

  Object read() throws Exception {
    return id2typeHandler.get(reader.readVarInt()).read(this);
  }

}

public final class JsFastSerializer extends AbstractFastSerializer {

  private void addClass(final int typeId, final Class<?> type, final boolean referenceable) {
    checkClass(type);
    final List<Field> fields = allFields(type);
    final Map<String, Field> name2field = new HashMap<>(fields.size());
    for (final Field field : fields) {
      final Field oldField = name2field.put(field.getName(), field);
      if (oldField != null) {
        throw new IllegalArgumentException("duplicated fields '" + field + "' and '" + oldField + "' in class hierarchy");
      }
    }
    Collections.sort(fields, new Comparator<Field>() {
      @Override public int compare(final Field field1, final Field field2) {
        return field1.getName().compareTo(field2.getName());
      }
    });
    final Map<Integer, Field> id2field = new HashMap<>(fields.size());
    int fieldId = FieldHandler.FIRST_ID;
    for (final Field field : fields) {
      id2field.put(fieldId++, field);
    }
    addClass(typeId, type, referenceable, id2field);
  }

  public static final TypeDesc BOOLEAN_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID, BaseTypeHandlers.BOOLEAN);
  public static final TypeDesc INTEGER_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 1, BaseTypeHandlers.INTEGER);
  public static final TypeDesc STRING_TYPEDESC = new TypeDesc(TypeDesc.FIRST_ID + 2, BaseTypeHandlers.STRING);
  private static final int FIRST_ID = TypeDesc.FIRST_ID + 3;

  public JsFastSerializer(
    final Reflector.Factory reflectorFactory, final Collection<Class<?>> enumerations,
    final Collection<Class<?>> concreteClasses, final Collection<Class<?>> referenceableConcreteClasses
  ) {
    super(reflectorFactory);
    addBaseType(BOOLEAN_TYPEDESC);
    addBaseType(INTEGER_TYPEDESC);
    addBaseType(STRING_TYPEDESC);
    int id = FIRST_ID;
    for (final Class<?> type : enumerations) {
      addEnum(id++, type);
    }
    for (final Class<?> type : concreteClasses) {
      addClass(id++, type, false);
    }
    for (final Class<?> type : referenceableConcreteClasses) {
      addClass(id++, type, true);
    }
    fixupFields();
  }

}

final class Output {

  final Writer writer;
  private final Map<Class<?>, TypeDesc> class2typeDesc;
  Map<Object, Integer> object2reference;

  Output(final Writer writer, final Map<Class<?>, TypeDesc> class2typeDesc) {
    this.writer = writer;
    this.class2typeDesc = class2typeDesc;
  }

  void write(@Nullable final Object value) throws Exception {
    if (value == null) {
      TypeDesc.NULL.write(null, this);
    } else if (value instanceof List) {
      TypeDesc.LIST.write(value, this);
    } else {
      final TypeDesc typeDesc = class2typeDesc.get(value.getClass());
      if (typeDesc == null) {
        throw new IllegalArgumentException("missing type '" + value.getClass().getCanonicalName() + '\'');
      }
      typeDesc.write(value, this);
    }
  }

}

public final class TypeDesc {

  public final int id;
  public final TypeHandler handler;

  public TypeDesc(final int id, final TypeHandler handler) {
    this.handler = Check.notNull(handler);
    if (id < 0) {
      throw new IllegalArgumentException("id " + id + " for type '" + handler.type.getCanonicalName() + "' must be >= 0");
    }
    this.id = id;
  }

  void write(final Object value, final Output output) throws Exception {
    handler.write(id, value, output);
  }

  static final TypeDesc NULL = new TypeDesc(0, new TypeHandler(Void.class) {
    @Override Object read(final Input input) {
      return null;
    }
    @Override void write(final Object value, final Output output) {
      // empty
    }
  });

  static final TypeDesc REFERENCE = new TypeDesc(1, new TypeHandler(Object.class) { // was reference
    @Override Object read(final Input input) throws Exception {
      return input.referenceableObjects.get(input.reader.readVarInt());
    }
    @Override void write(final Object value, final Output output) throws Exception {
      output.writer.writeVarInt((Integer)value);
    }
  });

  static final TypeDesc LIST = new TypeDesc(2, new TypeHandler(List.class) {
    @Override Object read(final Input input) throws Exception {
      int length = input.reader.readVarInt();
      final List<Object> list = new ArrayList<>(Math.min(length, 256)); // note: prevents out-of-memory attack
      while (length-- > 0) {
        list.add(input.read());
      }
      return list;
    }
    @Override void write(final Object value, final Output output) throws Exception {
      final List<Object> list = (List<Object>)value;
      output.writer.writeVarInt(list.size());
      for (final Object e : list) {
        output.write(e);
      }
    }
  });

  public static final int FIRST_ID = 3;

}

public abstract class TypeHandler {

  public final Class<?> type;

  TypeHandler(final Class<?> type) {
    this.type = Check.notNull(type);
  }

  abstract Object read(Input input) throws Exception;

  abstract void write(Object value, Output output) throws Exception;

  void write(final int id, final Object value, final Output output) throws Exception {
    output.writer.writeVarInt(id);
    write(value, output);
  }

}

public final class MessageSerializer implements Serializer {

  private final Serializer serializer;

  public MessageSerializer(final Serializer serializer) {
    this.serializer = Check.notNull(serializer);
  }

  private static final byte REQUEST = (byte)0;
  private static final byte VALUE_REPLY = (byte)1;
  private static final byte EXCEPTION_REPLY = (byte)2;

  private static Object[] toArray(final List<Object> list) {
    return list.toArray(new Object[list.size()]);
  }

  @Override public Message read(final Reader reader) throws Exception {
    final byte type = reader.readByte();
    if (type == REQUEST) {
      return new Request(
        serializer.read(reader),
        serializer.read(reader),
        toArray((List)serializer.read(reader))
      );
    }
    if (type == VALUE_REPLY) {
      return new ValueReply(
        serializer.read(reader)
      );
    }
    return new ExceptionReply(
      (Throwable)serializer.read(reader)
    );
  }

  private static final List<?> NO_ARGUMENTS = Collections.emptyList();

  @Override public void write(final Object message, final Writer writer) throws Exception {
    if (message instanceof Request) {
      writer.writeByte(REQUEST);
      final Request request = (Request)message;
      serializer.write(request.serviceId, writer);
      serializer.write(request.methodId, writer);
      serializer.write((request.arguments == null) ? NO_ARGUMENTS : Arrays.asList(request.arguments), writer);
    } else if (message instanceof ValueReply) {
      writer.writeByte(VALUE_REPLY);
      final ValueReply reply = (ValueReply)message;
      serializer.write(reply.value, writer);
    } else {
      writer.writeByte(EXCEPTION_REPLY);
      final ExceptionReply reply = (ExceptionReply)message;
      serializer.write(reply.throwable, writer);
    }
  }

}

public final class PacketSerializer implements Serializer {

  private final Serializer messageSerializer;

  public PacketSerializer(final Serializer messageSerializer) {
    this.messageSerializer = Check.notNull(messageSerializer);
  }

  @Override public Packet read(final Reader reader) throws Exception {
    final int requestNumber = reader.readInt();
    return Packet.isEnd(requestNumber) ? Packet.END : new Packet(requestNumber, (Message)messageSerializer.read(reader));
  }

  @Override public void write(final Object value, final Writer writer) throws Exception {
    final Packet packet = (Packet)value;
    if (packet.isEnd()) {
      writer.writeInt(Packet.END_REQUEST_NUMBER);
    } else {
      writer.writeInt(packet.requestNumber());
      messageSerializer.write(packet.message(), writer);
    }
  }

}

public final class WsConnection extends Connection {

  private volatile Session session;
  private final Serializer packetSerializer;
  public final javax.websocket.Session wsSession;
  private volatile RemoteEndpoint.Async remoteEndpoint;

  public WsConnection(
    final SessionSetup setup, final Serializer packetSerializer,
    final Thread.UncaughtExceptionHandler createSessionExceptionHandler,
    final javax.websocket.Session wsSession
  ) {
    this.packetSerializer = Check.notNull(packetSerializer);
    Check.notNull(createSessionExceptionHandler);
    this.wsSession = Check.notNull(wsSession);
    try {
      remoteEndpoint = wsSession.getAsyncRemote(); // $todo: implement batching ? setting send timeout ?
      session = setup.createSession(this);
    } catch (final Exception e) {
      try {
        wsSession.close();
      } catch (final Exception e2) {
        e.addSuppressed(e2);
      }
      createSessionExceptionHandler.uncaughtException(Thread.currentThread(), e);
      return;
    }
    if (!open(session)) {
      return;
    }
    wsSession.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
      @Override public void onMessage(final ByteBuffer in) {
        try {
          received(session, (Packet)packetSerializer.read(Reader.create(in)));
        } catch (final Exception e) {
          close(session, e);
        }
      }
    });
  }

  @Override protected void write(final Packet packet) throws Exception {
    new ByteArrayOutputStream(1024) {
      {
        packetSerializer.write(packet, Writer.create(this));
        remoteEndpoint.sendBinary(ByteBuffer.wrap(buf, 0, count), new SendHandler() {
          @Override public void onResult(final SendResult result) {
            if (result == null) {
              onError(new Exception("result == null"));
            } else if (!result.isOK()) {
              final Throwable throwable = result.getException();
              onError((throwable == null) ? new Exception("throwable == null") : throwable);
            }
          }
        });
      }
    };
  }

  @Override protected void closed() throws IOException {
    wsSession.close();
  }

  void onClose(final CloseReason closeReason) {
    onError(new Exception((closeReason == null) ? "closeReason == null" : closeReason.toString()));
  }

  void onError(final Throwable throwable) {
    if (session != null) {
      close(session, (throwable == null) ? new Exception("throwable == null") : throwable);
    }
  }

}
