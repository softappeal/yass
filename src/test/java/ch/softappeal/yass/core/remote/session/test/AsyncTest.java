package ch.softappeal.yass.core.remote.session.test;

import ch.softappeal.yass.core.remote.Client;
import ch.softappeal.yass.core.remote.Completer;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.InterceptorAsync;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.core.remote.SimpleInterceptorContext;
import ch.softappeal.yass.core.remote.SimpleMethodMapper;
import ch.softappeal.yass.core.remote.session.SimpleSession;
import ch.softappeal.yass.core.test.InvokeTest;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Exceptions;
import ch.softappeal.yass.util.NamedThreadFactory;
import ch.softappeal.yass.util.Nullable;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AsyncTest {

    public interface TestService {
        void noResult();
        int divide(int a, int b) throws InvokeTest.DivisionByZeroException;
        Integer getInteger();
        String getString();
        @OneWay void oneWay();
    }

    private static void println(final String name, final String type, final String method, final @Nullable Integer id, final Object message) {
        System.out.printf(
            "%10s | %7s | %9s | %10s | %11s | %2s | %s\n",
            System.nanoTime() / 1000000L, name, type, Thread.currentThread().getName(), method, id == null ? "" : id, message
        );
    }

    private static void sleep(final Consumer<Completer> execute) {
        final Completer completer = Server.completer();
        new Thread() {
            @Override public void run() {
                try {
                    TimeUnit.MILLISECONDS.sleep(1000L);
                } catch (final InterruptedException e) {
                    throw new RuntimeException(e);
                }
                execute.accept(completer);
            }
        }.start();
    }

    private static final class TestServiceImpl implements TestService {
        @Override public void noResult() {
            sleep(completer -> completer.complete(null));
            println("impl", "", "noResult", null, "");
        }
        @Override public int divide(final int a, final int b) {
            sleep(completer -> {
                if (b == 0) {
                    completer.completeExceptionally(new InvokeTest.DivisionByZeroException(a));
                } else {
                    completer.complete(a / b);
                }
            });
            println("impl", "", "divide", null, a + "/" + b);
            return 0;
        }
        @Override public Integer getInteger() {
            sleep(completer -> completer.complete(123));
            println("impl", "", "getInteger", null, "");
            return null;
        }
        @Override public String getString() {
            sleep(completer -> completer.complete("string"));
            println("impl", "", "getString", null, "");
            return null;
        }
        @Override public void oneWay() {
            try {
                Server.completer();
                Assert.fail();
            } catch (final IllegalStateException ignore) {
                // empty
            }
            println("impl", "", "oneWay", null, "");
        }
    }

    private static final class Logger implements InterceptorAsync<SimpleInterceptorContext> {
        private final String name;
        Logger(final String name) {
            this.name = Check.notNull(name);
        }
        @Override public SimpleInterceptorContext entry(final MethodMapper.Mapping methodMapping, final @Nullable Object[] arguments) {
            final SimpleInterceptorContext context = new SimpleInterceptorContext(methodMapping, arguments);
            println(name, "entry", methodMapping.method.getName(), context.id, Arrays.deepToString(arguments));
            return context;
        }
        @Override public void exit(final SimpleInterceptorContext context, final @Nullable Object result) {
            println(name, "exit", context.methodMapping.method.getName(), context.id, result);
        }
        @Override public void exception(final SimpleInterceptorContext context, final Exception exception) {
            println(name, "exception", context.methodMapping.method.getName(), context.id, exception);
        }
    }

    private static final ContractId<TestService> ID = ContractId.create(TestService.class, 987654, SimpleMethodMapper.FACTORY);

    @Test public void inactive() {
        try {
            Server.completer();
            Assert.fail();
        } catch (final IllegalStateException e) {
            System.out.println(e);
        }
    }

    @Test public void test() throws InterruptedException {
        final ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        try {
            LocalConnection.connect(
                connection -> new SimpleSession(connection, executor) {
                    @Override protected void closed(final @Nullable Exception exception) {
                        System.out.println("client closed: " + exception);
                    }
                    @Override protected void opened() {
                        final TestService test = proxyAsync(ID, new Logger("client"));
                        Client.promise(test::noResult).thenAccept(r -> println("proxy", "", "noResult", null, r));
                        Client.promise(() -> test.divide(12, 3)).thenAccept(r -> println("proxy", "", "divide", null, r));
                        Client.promise(() -> test.divide(12, 4)).thenAcceptAsync(r -> println("proxy", "", "divide", null, r), executor);
                        Client.promise(() -> test.divide(123, 0)).whenComplete((r, e) -> println("proxy", "", "divide", null, e));
                        Client.promise(test::getInteger).thenAccept(r -> println("proxy", "", "getInteger", null, r.intValue()));
                        Client.promise(test::getString).thenAccept(r -> println("proxy", "", "getString", null, r.substring(0)));
                        test.oneWay();
                        try {
                            Client.promise(test::oneWay);
                            Assert.fail();
                        } catch (final IllegalStateException ignore) {
                            // empty
                        }
                        try {
                            test.noResult();
                            Assert.fail();
                        } catch (final IllegalStateException ignore) {
                            // empty
                        }
                        println("------", "", "", null, "");
                    }
                },
                connection -> new SimpleSession(connection, executor) {
                    @Override protected void closed(final @Nullable Exception exception) {
                        System.out.println("server closed: " + exception);
                    }
                    @Override protected Server server() throws Exception {
                        return new Server(ID.serviceAsync(new TestServiceImpl(), new Logger("server")));
                    }
                }
            );
            TimeUnit.MILLISECONDS.sleep(500L);
            println("======", "", "", null, "");
            TimeUnit.MILLISECONDS.sleep(1000L);
        } finally {
            executor.shutdown();
        }
    }

}
