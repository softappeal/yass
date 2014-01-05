package ch.softappeal.yass.tutorial.contract.instrument;

import ch.softappeal.yass.tutorial.contract.Instrument;
import ch.softappeal.yass.util.Tag;

@Tag(31) public final class Bond extends Instrument {

  @Tag(10) public final int coupon;

  public Bond(final String id, final String name, final int coupon) {
    super(id, name);
    this.coupon = coupon;
  }

}
