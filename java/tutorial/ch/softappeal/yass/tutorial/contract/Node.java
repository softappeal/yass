package ch.softappeal.yass.tutorial.contract;

import java.util.ArrayList;
import java.util.List;

public final class Node {

    public final double id;
    public final List<Node> links = new ArrayList<>();

    public Node(final double id) {
        this.id = id;
    }

}
