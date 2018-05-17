package ch.softappeal.yass.serialize.contractj;

import ch.softappeal.yass.Tag;

import java.io.Serializable;

@Tag(50) public final class Node implements Serializable {

    private static final long serialVersionUID = 1L;

    // needed for coverage tests; these fields won't be serialized
    public transient int transientInt;
    public static int staticInt;

    @Tag(1) public int id;
    @Tag(2) public Node link;

    public Node(final int id) {
        this.id = id;
    }

    public Node() {
        // empty
    }

}
