package ch.softappeal.yass.javadir;

import ch.softappeal.yass.Tag;

@Tag(1)
public class JavaTaggedClass {

    @Tag(2)
    int tag;

    @Tag(3)
    public void withTag() {
        // empty
    }

    public void noTag() {
        // empty
    }

}
