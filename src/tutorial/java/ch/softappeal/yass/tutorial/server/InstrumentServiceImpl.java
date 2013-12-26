package ch.softappeal.yass.tutorial.server;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.Stock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InstrumentServiceImpl implements InstrumentService {

  static final Map<String, Instrument> INSTRUMENTS;

  static {
    final List<String> names = Arrays.asList("IBM", "Google", "Apple", "Microsoft");
    final Map<String, Instrument> instruments = new HashMap<>(names.size());
    int index = 0;
    for (final String name : names) {
      final String id = String.valueOf(index++);
      instruments.put(id, new Stock(id, name, true));
    }
    INSTRUMENTS = Collections.unmodifiableMap(instruments);
  }

  @Override public List<Instrument> getInstruments() {
    return new ArrayList<>(INSTRUMENTS.values());
  }

}
