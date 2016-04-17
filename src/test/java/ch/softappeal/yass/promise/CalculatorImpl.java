package ch.softappeal.yass.promise;

import ch.softappeal.yass.promise.contract.promise.Calculator;

import java.util.concurrent.CompletionStage;

public final class CalculatorImpl extends Promise<ch.softappeal.yass.promise.contract.Calculator> implements Calculator {

    public CalculatorImpl(ch.softappeal.yass.promise.contract.Calculator proxy) {
        super(proxy);
    }

    @Override public void oneWay() {
        proxy.oneWay();
    }

    @Override public CompletionStage<Integer> divide(final int a, final int b) {
        return create(() -> proxy.divide(a, b));
    }

    @Override public CompletionStage<Void> noResult() {
        return create(() -> proxy.noResult());
    }

}
