package ch.softappeal.yass.serialize.contract;

import ch.softappeal.yass.Tag;

@Tag(120) public final class C1 {

    @Tag(1) public final int i1;

    private C1() {
        i1 = 0;
    }

    public C1(final int i1) {
        this.i1 = i1;
    }

}
