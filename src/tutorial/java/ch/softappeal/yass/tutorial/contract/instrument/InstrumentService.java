package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.tutorial.contract.Instrument;

import java.util.List;

public interface InstrumentService {

    List<Instrument> getInstruments();

    @OneWay void reload(boolean testBoolean, int testInt);

}
