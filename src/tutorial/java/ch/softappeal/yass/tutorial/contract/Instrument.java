package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Check;

public abstract class Instrument {

    public final int id;
    public final String name;

    protected Instrument(final int id, final String name) {
        this.id = id;
        this.name = Check.notNull(name);
    }

}
