package ch.softappeal.yass.tutorial.contract;

import ch.softappeal.yass.util.Tag;

import java.util.List;

public interface InstrumentService {

  @Tag(0) List<Instrument> getInstruments();

}
