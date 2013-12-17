package ch.softappeal.yass.serialize.contract.nested;

import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.contract.PrimitiveTypes;
import ch.softappeal.yass.util.Tag;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

@Tag(21) public final class AllTypes extends PrimitiveTypes {

  private static final long serialVersionUID = 1L;

  @Tag(100) public String stringField;

  @Tag(101) public Color colorField;

  @Tag(102) public BigDecimal bigDecimalField;
  @Tag(103) public BigInteger bigIntegerField;

  @Tag(106) public PrimitiveTypes primitiveTypesField;
  @Tag(107) public List<PrimitiveTypes> primitiveTypesListField;

  @Tag(108) public Object objectField;
  @Tag(109) public List<Object> objectListField;

  @Tag(110) public Throwable throwable;

  public AllTypes(final String stringField) {
    this.stringField = stringField;
  }

  public AllTypes() {
    // empty
  }

}
