package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.core.remote.OneWay;
import ch.softappeal.yass.util.Tag;

import java.util.List;

public interface InstrumentService {

  @Tag(1) List<Instrument> getInstruments();

  @Tag(2) @OneWay void reload();

}
