package ch.softappeal.yass.tutorial.acceptor;

import ch.softappeal.yass.remote.*;
import ch.softappeal.yass.tutorial.contract.*;
import ch.softappeal.yass.tutorial.contract.instrument.*;
import ch.softappeal.yass.tutorial.contract.instrument.stock.*;

import java.util.*;

import static ch.softappeal.yass.remote.ServerKt.*;

public final class InstrumentServiceImpl implements InstrumentService {

    static final Map<Integer, Instrument> INSTRUMENTS;

    static {
        final List<String> names = Arrays.asList("IBM", "Google", "Apple", "Microsoft");
        final Map<Integer, Instrument> instruments = new HashMap<>(names.size());
        int index = 0;
        for (final String name : names) {
            final int id = index++;
            instruments.put(id, new Stock(id, name, true));
        }
        INSTRUMENTS = new HashMap<>(instruments);
    }

    private InstrumentServiceImpl() {
        // disable
    }

    @Override
    public List<Instrument> getInstruments() {
        final Completer completer = getCompleter();
        new Thread(() -> completer.complete(new ArrayList<>(INSTRUMENTS.values())))
            .start(); // setting result asynchronously
        return null; // needed for compiler; returned result is not used
    }

    @Override
    public void showOneWay(final boolean testBoolean, final int testInt) {
        // empty
    }

    public static final InstrumentService INSTANCE = new InstrumentServiceImpl();

}
