package ch.softappeal.yass.tutorial.simple;

import ch.softappeal.yass.core.Interceptors;

public final class Main {

  public static void main(final String... args) {
    final Calculator calculator = Interceptors.proxy(Calculator.class, new CalculatorImpl(), new Logger());
    System.out.println(calculator.add(2, 3));
  }

}
