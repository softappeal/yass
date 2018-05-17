package ch.softappeal.yass.serialize.contractj;

import ch.softappeal.yass.Tag;

import java.io.Serializable;

@Tag(40) public class PrimitiveTypes implements Serializable {

    private static final long serialVersionUID = 1L;

    @Tag(28) public boolean booleanField;
    @Tag(1) public byte byteField;
    @Tag(2) public short shortField;
    @Tag(3) public int intField;
    @Tag(4) public long longField;
    @Tag(5) public char charField = ' ';
    @Tag(6) public float floatField;
    @Tag(7) public double doubleField;

    @Tag(10) public boolean[] booleanArrayField;
    @Tag(11) public byte[] byteArrayField;
    @Tag(12) public short[] shortArrayField;
    @Tag(13) public int[] intArrayField;
    @Tag(14) public long[] longArrayField;
    @Tag(15) public char[] charArrayField;
    @Tag(16) public float[] floatArrayField;
    @Tag(17) public double[] doubleArrayField;

    @Tag(20) public Boolean booleanWrapperField;
    @Tag(21) public Byte byteWrapperField;
    @Tag(22) public Short shortWrapperField;
    @Tag(23) public Integer intWrapperField;
    @Tag(24) public Long longWrapperField;
    @Tag(25) public Character charWrapperField;
    @Tag(26) public Float floatWrapperField;
    @Tag(27) public Double doubleWrapperField;

    public PrimitiveTypes(final int intField) {
        this.intField = intField;
    }

    public PrimitiveTypes() {
        // empty
    }

}
