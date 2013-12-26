package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.Interceptors;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.TaggedMethodMapper;
import ch.softappeal.yass.core.remote.session.Connection;
import ch.softappeal.yass.core.remote.session.LocalConnection;
import ch.softappeal.yass.core.remote.session.Session;
import ch.softappeal.yass.core.remote.session.SessionFactory;
import ch.softappeal.yass.core.remote.session.SessionSetup;
import ch.softappeal.yass.core.remote.test.ContractIdTest;
import ch.softappeal.yass.core.remote.test.RemoteTest;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.transport.socket.SocketListenerTest;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LocalConnectionTest extends InvokeTest {

  public static SessionSetup createSetup(
    final boolean invoke, final String name, final Executor requestExecutor,
    final boolean createException, final boolean openedException, final boolean invokeBeforeOpened
  ) {
    return new SessionSetup(
      new Server(
        TaggedMethodMapper.FACTORY,
        ContractIdTest.ID.service(
          new TestServiceImpl(),
          invoke ? RemoteTest.CONTRACT_ID_CHECKER : Interceptors.composite(RemoteTest.CONTRACT_ID_CHECKER, SERVER_INTERCEPTOR)
        )
      ),
      requestExecutor,
      new SessionFactory() {
        @Override public Session create(final SessionSetup setup, final Connection connection) throws Exception {
          if (createException) {
            throw new Exception("create failed");
          }
          return new Session(setup, connection) {
            {
              println(name, "create", hashCode());
              if (invokeBeforeOpened) {
                ContractIdTest.ID.invoker(this).proxy().nothing();
              }
            }
            @Override public void opened() throws Exception {
              println(name, "opened", hashCode());
              if (openedException) {
                throw new Exception("opened failed");
              }
              if (invoke) {
                try (Session session = this) {
                  InvokeTest.invoke(
                    ContractIdTest.ID.invoker(session).proxy(
                      invoke ? Interceptors.composite(PRINTLN_AFTER, RemoteTest.CONTRACT_ID_CHECKER, CLIENT_INTERCEPTOR) : RemoteTest.CONTRACT_ID_CHECKER
                    )
                  );
                }
              }
            }
            @Override public void closed(@Nullable final Exception exception) {
              if (invoke) {
                Assert.assertNull(exception);
              }
              println(name, "closed", hashCode() + " " + exception);
            }
          };
        }
      }
    );
  }

  @Test public void plain() throws InterruptedException {
    try {
      Session.get();
      Assert.fail();
    } catch (final RuntimeException e) {
      Assert.assertEquals("no active invocation", e.getMessage());
    }
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      LocalConnection.connect(createSetup(true, "client", executor, false, false, false), createSetup(false, "server", executor, false, false, false));
      TimeUnit.MILLISECONDS.sleep(400L);
      System.out.println();
      LocalConnection.connect(createSetup(false, "client", executor, false, false, false), createSetup(true, "server", executor, false, false, false));
      TimeUnit.MILLISECONDS.sleep(400L);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test public void createException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      try {
        LocalConnection.connect(createSetup(false, "client", executor, false, false, false), createSetup(false, "server", executor, true, false, false));
        Assert.fail();
      } catch (final RuntimeException e) {
        Assert.assertEquals(e.getMessage(), "java.lang.Exception: create failed");
      }
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void invokeBeforeOpened() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      try {
        LocalConnection.connect(createSetup(false, "client", executor, false, false, false), createSetup(false, "server", executor, false, false, true));
        Assert.fail();
      } catch (final RuntimeException e) {
        Assert.assertEquals(e.getMessage(), "session is not yet opened");
      }
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

  @Test public void openedException() throws InterruptedException {
    final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", TestUtils.TERMINATE));
    try {
      LocalConnection.connect(createSetup(false, "client", executor, false, true, false), createSetup(false, "server", executor, false, true, false));
      TimeUnit.MILLISECONDS.sleep(100L);
    } finally {
      SocketListenerTest.shutdown(executor);
    }
  }

}
