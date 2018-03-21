package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.core.remote.Server;
import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.tutorial.contract.instrument.InstrumentService;
import ch.softappeal.yass.tutorial.contract.instrument.stock.Stock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InstrumentServiceImplAsync implements InstrumentService {

    static final Map<Integer, Instrument> INSTRUMENTS;
    static {
        final var names = List.of("IBM", "Google", "Apple", "Microsoft");
        final Map<Integer, Instrument> instruments = new HashMap<>(names.size());
        var index = 0;
        for (final var name : names) {
            final var id = index++;
            instruments.put(id, new Stock(id, name, true));
        }
        INSTRUMENTS = Map.copyOf(instruments);
    }

    private InstrumentServiceImplAsync() {
        // disable
    }

    @Override public List<Instrument> getInstruments() {
        final var completer = Server.completer();
        new Thread(() -> completer.complete(new ArrayList<>(INSTRUMENTS.values()))).start(); // setting result asynchronously
        return null; // needed for compiler; returned result is not used
    }

    @Override public void showOneWay(final boolean testBoolean, final int testInt) {
        // empty
    }

    public static final InstrumentService INSTANCE = new InstrumentServiceImplAsync();

}
