package ch.softappeal.yass;

@SuppressWarnings("UnusedDeclaration")
public final class Version {

  private Version() {
    // disable
  }

  public static final String VALUE = Version.class.getPackage().getImplementationVersion();

}
