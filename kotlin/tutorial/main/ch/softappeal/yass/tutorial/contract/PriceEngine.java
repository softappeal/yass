package ch.softappeal.yass.tutorial.contract;

import java.util.*;

public interface PriceEngine {

    void subscribe(List<Integer> instrumentIds) throws UnknownInstrumentsException;

}
