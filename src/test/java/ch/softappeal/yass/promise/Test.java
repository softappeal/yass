package ch.softappeal.yass.promise;

import ch.softappeal.yass.promise.contract.promise.Calculator;

public class Test {

    private static void print(final String message) {
        System.out.println(message + " " + Thread.currentThread().getName());
    }

    public static void main(final String... args) {
        final Calculator calculator = new CalculatorSimulation();
        calculator.divide(12, 3).thenAccept(r -> print("result: " + r));
        calculator.noResult().thenAccept(r -> print("noResult: " + r));
        calculator.divide(12, 0).whenComplete((r, e) -> print("exception: " + e));
        calculator.divide(12, 4).thenAcceptAsync(r -> print("result: " + r));
        System.out.println("done");
    }

}
