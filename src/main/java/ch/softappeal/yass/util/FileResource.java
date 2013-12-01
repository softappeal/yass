package ch.softappeal.yass.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public final class FileResource implements Resource {

  private final File file;

  @SuppressWarnings("WeakerAccess")
  public FileResource(final File file) {
    this.file = Check.notNull(file);
  }

  public FileResource(final String file) {
    this(new File(file));
  }

  @Override public InputStream create() {
    try {
      return new FileInputStream(file);
    } catch (final FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

}
