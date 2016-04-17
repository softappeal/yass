package ch.softappeal.yass.promise.contract;

import ch.softappeal.yass.core.remote.OneWay;

public interface Calculator {

    @OneWay void oneWay();

    int divide(int a, int b) throws DivideByZeroException;

    void noResult();

}
