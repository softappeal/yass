package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Tag;

import java.util.List;

public interface InstrumentService {

    @Tag(0) List<Instrument> getInstruments();

    /**
     * This method does nothing meaningful.
     * It just shows how to make {@link OneWay} method calls.
     */
    @Tag(1) @OneWay void reload(boolean testBoolean, int testInt);

}
