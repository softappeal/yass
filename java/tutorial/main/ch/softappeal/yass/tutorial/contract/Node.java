package ch.softappeal.yass.tutorial.contract;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows how to use graphs.
 */
public final class Node {

    public final double id;
    public final List<Node> links = new ArrayList<>();
    public Node next;

    public Node(final double id) {
        this.id = id;
    }

}
