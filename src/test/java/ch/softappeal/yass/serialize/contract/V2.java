package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.util.Nullable;
import ch.softappeal.yass.util.Tag;

@Tag(120) public final class V2 {

  @Tag(1) public final int i1;
  @Tag(2) @Nullable public final Integer i2;

  public V2(final int i1, final int i2) {
    this.i1 = i1;
    this.i2 = i2;
  }

  public int i2() {
    return (i2 == null) ? 13 : i2;
  }

}
