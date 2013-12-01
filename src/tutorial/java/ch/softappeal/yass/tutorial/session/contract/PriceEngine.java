package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.util.Tag;

import java.util.List;

public interface PriceEngine {

  @Tag(0) void subscribe(List<String> instrumentIds) throws UnknownInstrumentsException;

}
