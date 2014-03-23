package ch.softappeal.yass.util;

import java.io.InputStream;

@FunctionalInterface
public interface Resource {

  InputStream create();

}
