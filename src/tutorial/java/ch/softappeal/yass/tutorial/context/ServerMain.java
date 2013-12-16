package ch.softappeal.yass.tutorial.context;

import ch.softappeal.yass.core.ContextInterceptor;
import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.MethodMappers;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.Service;
import ch.softappeal.yass.serialize.JavaSerializer;
import ch.softappeal.yass.serialize.Serializer;
import ch.softappeal.yass.transport.MessageSerializer;
import ch.softappeal.yass.transport.socket.SslSetup;
import ch.softappeal.yass.transport.socket.StatelessTransport;
import ch.softappeal.yass.util.ClassLoaderResource;
import ch.softappeal.yass.util.ContextTransformer;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ServerMain {

  private static final char[] PASSWORD = "changeit".toCharArray();
  private static KeyStore readKeyStore(final String name) {
    return SslSetup.readKeyStore(
      new ClassLoaderResource(ServerMain.class.getClassLoader(), ServerMain.class.getPackage().getName().replace('.', '/') + '/' + name + ".jks"),
      PASSWORD
    );
  }
  public static final SslSetup SSL_SETUP = new SslSetup(
    "TLSv1.2", "TLS_RSA_WITH_AES_128_CBC_SHA", readKeyStore("Test"), PASSWORD, readKeyStore("TestCA")
  );

  public static final SocketAddress ADDRESS = new InetSocketAddress("localhost", 28947);
  public static final Serializer SERIALIZER = new MessageSerializer(JavaSerializer.INSTANCE);
  public static final MethodMapper.Factory METHOD_MAPPER_FACTORY = MethodMappers.STRING_FACTORY;
  public static final ContractId<Account> ACCOUNT_ID = ContractId.create(Account.class, "Account");

  public static void main(final String... args) {
    final Executor executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.STD_ERR));
    final Service service = ACCOUNT_ID.service(
      new AccountImpl(
        new ContextTransformer<Context, String>(ContextInterceptor.<Context>get()) { // used for incoming context
          @Override protected String transform(final Context fromContext) {
            return fromContext.user;
          }
        }
      ),
      ContextInterceptor.get(), // used for incoming context
      new Logger("service"),
      new Interceptor() { // used for outgoing context
        @Override public Object invoke(final Invocation invocation) throws Throwable {
          try {
            return invocation.proceed();
          } finally {
            invocation.context = new Date(); // sets outgoing context
          }
        }
      }
    );
    new StatelessTransport(
      new Server(METHOD_MAPPER_FACTORY, service),
      SERIALIZER,
      executor,
      Exceptions.STD_ERR
    ).start(executor, SSL_SETUP.serverSocketFactory, ADDRESS);
    System.out.println("started");
  }

}
