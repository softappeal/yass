package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.tutorial.contract.*;

import java.util.*;

public interface InstrumentService {

    List<Instrument> getInstruments();

    /**
     * This method does nothing meaningful.
     * It just shows how to make {@link OneWay} method calls.
     */
    @OneWay
    void showOneWay(boolean testBoolean, int testInt);

}
