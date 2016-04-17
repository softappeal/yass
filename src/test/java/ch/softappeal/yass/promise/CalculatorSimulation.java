package ch.softappeal.yass.promise;

import ch.softappeal.yass.promise.contract.DivideByZeroException;
import ch.softappeal.yass.promise.contract.promise.Calculator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public final class CalculatorSimulation implements Calculator {

    private static void doSleep(final String message) {
        System.out.println(message + " " + Thread.currentThread().getName());
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public void oneWay() {
        // empty
    }

    @Override public CompletionStage<Integer> divide(final int a, final int b) {
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        new Thread() {
            @Override public void run() {
                doSleep("divide " + a + " " + b);
                if (b == 0) {
                    future.completeExceptionally(new DivideByZeroException());
                } else {
                    future.complete(a / b);
                }

            }
        }.start();
        return future;
    }

    @Override public CompletionStage<Void> noResult() {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        new Thread() {
            @Override public void run() {
                doSleep("noResult");
                future.complete(null);

            }
        }.start();
        return future;
    }

}
