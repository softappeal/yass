package ch.softappeal.yass.promise.contract.promise;

import ch.softappeal.yass.core.remote.OneWay;

import java.util.concurrent.CompletionStage;

public interface Calculator {

    @OneWay void oneWay();

    CompletionStage<Integer> divide(int a, int b);

    CompletionStage<Void> noResult();

}
