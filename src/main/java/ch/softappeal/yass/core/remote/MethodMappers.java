package ch.softappeal.yass.core.remote;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class MethodMappers {

    private MethodMappers() {
        // disable
    }

    public static void print(final PrintWriter printer, final MethodMapper.Factory factory, final Class<?> contract) {
        final MethodMapper methodMapper = factory.create(contract);
        final List<MethodMapper.Mapping> mappings = new ArrayList<>();
        for (final Method method : contract.getMethods()) {
            mappings.add(methodMapper.mapMethod(method));
        }
        Collections.sort(mappings, new Comparator<MethodMapper.Mapping>() {
            @Override public int compare(final MethodMapper.Mapping mapping1, final MethodMapper.Mapping mapping2) {
                return ((Integer)mapping1.id).compareTo((Integer)mapping2.id);
            }
        });
        for (final MethodMapper.Mapping mapping : mappings) {
            printer.println(mapping.id + ": " + mapping.method);
        }
    }

}
