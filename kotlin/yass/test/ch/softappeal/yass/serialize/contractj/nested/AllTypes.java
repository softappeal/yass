package ch.softappeal.yass.serialize.contractj.nested;

import ch.softappeal.yass.Tag;
import ch.softappeal.yass.serialize.contractj.Color;
import ch.softappeal.yass.serialize.contractj.PrimitiveTypes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Tag(41) public final class AllTypes extends PrimitiveTypes {

    private static final long serialVersionUID = 1L;

    @Tag(100) public String stringField;

    @Tag(101) public Color colorField;

    @Tag(102) public BigDecimal bigDecimalField;
    @Tag(103) public BigInteger bigIntegerField;
    @Tag(104) public Date dateField;
    @Tag(105) public Instant instantField;

    @Tag(106) public PrimitiveTypes primitiveTypesField;
    @Tag(107) public List<PrimitiveTypes> primitiveTypesListField;

    @Tag(108) public Object objectField;
    @Tag(109) public List<Object> objectListField;

    @Tag(110) public Exception exception;

    public AllTypes(final String stringField) {
        this.stringField = stringField;
    }

    public AllTypes() {
        // empty
    }

}
