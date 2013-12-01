package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.util.Tag;

import java.io.Serializable;

@Tag(30) public final class Node implements Serializable {

  private static final long serialVersionUID = 1L;

  // needed for coverage tests; these fields won't be serialized
  public transient int transientInt;
  public static int staticInt;

  @Tag(0) public int id;
  @Tag(1) public Node link;

  public Node(final int id) {
    this.id = id;
  }

  public Node() {
    // empty
  }

}
