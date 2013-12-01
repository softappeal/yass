package ch.softappeal.yass.tutorial.context;

import ch.softappeal.yass.core.Interceptor;
import ch.softappeal.yass.core.Invocation;
import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.transport.socket.StatelessTransport;

public final class ClientMain {

  public static void main(final String... args) {
    System.setProperty("javax.net.debug", "ssl");
    final Client client = StatelessTransport.client(
      ServerMain.METHOD_MAPPER_FACTORY, ServerMain.SERIALIZER, ServerMain.SSL_SETUP.socketFactory, ServerMain.ADDRESS
    );
    final Account account = ServerMain.ACCOUNT_ID.invoker(client).proxy(
      new Logger("proxy"),
      new Interceptor() { // used for context
        @Override public Object invoke(final Invocation invocation) throws Throwable {
          invocation.context = new Context("Alice", 123); // sets outgoing context
          try {
            return invocation.proceed();
          } finally {
            System.out.println("time = " + invocation.context); // gets incoming context
          }
        }
      }
    );
    System.out.println("balance = " + account.getBalance());
  }

}
