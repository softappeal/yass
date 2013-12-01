package ch.softappeal.yass.tutorial.session.contract;

import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Tag;

import java.util.List;

@Tag(3) public final class UnknownInstrumentsException extends Exception {

  private static final long serialVersionUID = 1L;

  @Tag(0) public final List<String> instrumentIds;

  public UnknownInstrumentsException(final List<String> instrumentIds) {
    this.instrumentIds = Check.notNull(instrumentIds);
  }

  /**
   * @see AbstractFastSerializer
   */
  @Override public String getMessage() {
    return "there are " + instrumentIds.size() + " unknown instruments";
  }

}
