package ch.softappeal.yass.remote.session.test;

import ch.softappeal.yass.Exceptions;
import ch.softappeal.yass.NamedThreadFactory;
import ch.softappeal.yass.Nullable;
import ch.softappeal.yass.Reference;
import ch.softappeal.yass.remote.session.Session;
import ch.softappeal.yass.remote.session.SessionWatcher;
import ch.softappeal.yass.remote.session.SimpleSession;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SessionWatcherTest {

    public static void main(final String... args) throws InterruptedException {
        final var executor = Executors.newCachedThreadPool(new NamedThreadFactory("executor", Exceptions.TERMINATE));
        final var latch = new CountDownLatch(1);
        final Reference<Session> session = Reference.create();
        LocalConnection.connect(
            connection -> new SimpleSession(connection, executor) {
                @Override protected void opened() {
                    System.out.println(this + " opened");
                    session.set(this);
                    latch.countDown();
                }
                @Override protected void closed(final @Nullable Exception exception) {
                    System.out.println(this + " closed " + exception);
                }
            },
            connection -> new SimpleSession(connection, executor) {
                @Override protected void opened() {
                    System.out.println(this + " opened");
                }
                @Override protected void closed(final @Nullable Exception exception) {
                    System.out.println(this + " closed " + exception);
                }
            }
        );
        latch.await();
        ExecutorService watcherExecutor;

        System.out.println("watcherExecutor up");
        watcherExecutor = Executors.newFixedThreadPool(2, new NamedThreadFactory("watcherExecutor", Exceptions.TERMINATE));
        SessionWatcher.watchSession(watcherExecutor, session.get(), 2L, 1L, () -> System.out.println("check"));
        TimeUnit.SECONDS.sleep(10L);

        watcherExecutor.shutdownNow();
        System.out.println("watcherExecutor down");
        TimeUnit.SECONDS.sleep(10L);

        System.out.println("watcherExecutor up");
        watcherExecutor = Executors.newFixedThreadPool(2, new NamedThreadFactory("watcherExecutor", Exceptions.TERMINATE));
        SessionWatcher.watchSession(watcherExecutor, session.get(), 2L, 1L, () -> {
            throw new Exception("check exception");
        });
        TimeUnit.SECONDS.sleep(10L);

        watcherExecutor.shutdownNow();
        executor.shutdown();
    }

}
