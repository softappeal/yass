package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

public abstract class Instrument {

    @Tag(1) public final int id;
    @Tag(2) public final String name;

    protected Instrument(final int id, final String name) {
        this.id = id;
        this.name = Check.notNull(name);
    }

}
