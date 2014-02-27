package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.OneWay;

import java.util.List;

public interface InstrumentService {

  List<Instrument> getInstruments();

  @OneWay void reload(boolean testBoolean, int testInt);

}
