package ch.softappeal.yass.tutorial.session.server;

import ch.softappeal.yass.tutorial.session.contract.Config;
import ch.softappeal.yass.tutorial.session.contract.Instrument;
import ch.softappeal.yass.tutorial.session.contract.InstrumentService;

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
      instruments.put(id, new Instrument(id, name));
    }
    INSTRUMENTS = Collections.unmodifiableMap(instruments);
    System.out.println(Config.DUMPER.toString(INSTRUMENTS.values()));
  }

  @Override public List<Instrument> getInstruments() {
    return new ArrayList<>(INSTRUMENTS.values());
  }

}
