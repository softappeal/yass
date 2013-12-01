package ch.softappeal.yass.serialize.convert.test;

import ch.softappeal.yass.serialize.contract.Binary;
import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.contract.LongClass;
import ch.softappeal.yass.serialize.convert.BinaryTypeConverter;
import ch.softappeal.yass.serialize.convert.IntegerTypeConverter;
import ch.softappeal.yass.serialize.convert.LongTypeConverter;
import ch.softappeal.yass.serialize.convert.StringTypeConverter;
import ch.softappeal.yass.serialize.convert.TypeConverter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TypeConverterTest {

  private static final class Visitor implements TypeConverter.Visitor {

    TypeConverter typeConverter;
    boolean stringCalled = false;
    boolean binaryCalled = false;
    boolean integerCalled = false;
    boolean longCalled = false;

    @Override public void visit(final StringTypeConverter typeConverter) {
      this.typeConverter = typeConverter;
      stringCalled = true;
    }

    @Override public void visit(final BinaryTypeConverter typeConverter) {
      this.typeConverter = typeConverter;
      binaryCalled = true;
    }

    @Override public void visit(final IntegerTypeConverter typeConverter) {
      this.typeConverter = typeConverter;
      integerCalled = true;
    }

    @Override public void visit(final LongTypeConverter typeConverter) {
      this.typeConverter = typeConverter;
      longCalled = true;
    }

  }

  @Test public void visitor() {
    Visitor visitor;
    TypeConverter typeConverter;
    typeConverter = Binary.TYPE_CONVERTER;
    visitor = new Visitor();
    typeConverter.accept(visitor);
    Assert.assertSame(visitor.typeConverter, typeConverter);
    Assert.assertTrue(visitor.binaryCalled);
    typeConverter = StringTypeConverter.BIG_INTEGER;
    visitor = new Visitor();
    typeConverter.accept(visitor);
    Assert.assertSame(visitor.typeConverter, typeConverter);
    Assert.assertTrue(visitor.stringCalled);
    typeConverter = IntegerTypeConverter.forEnum(Color.class);
    visitor = new Visitor();
    typeConverter.accept(visitor);
    Assert.assertSame(visitor.typeConverter, typeConverter);
    Assert.assertTrue(visitor.integerCalled);
    typeConverter = LongClass.TYPE_CONVERTER;
    visitor = new Visitor();
    typeConverter.accept(visitor);
    Assert.assertSame(visitor.typeConverter, typeConverter);
    Assert.assertTrue(visitor.longCalled);

  }

  @Test public void binary() throws Exception {
    final BinaryTypeConverter typeConverter = Binary.TYPE_CONVERTER;
    Assert.assertSame(typeConverter.type, Binary.class);
    Assert.assertArrayEquals(new byte[] {(byte)1, (byte)2}, ((Binary)typeConverter.fromBinary(new byte[] {(byte)1, (byte)2})).value());
    Assert.assertArrayEquals(new byte[] {(byte)10, (byte)20}, typeConverter.toBinary(new Binary(new byte[] {(byte)10, (byte)20})));
  }

  @Test public void integer() throws Exception {
    final IntegerTypeConverter typeConverter = IntegerTypeConverter.forEnum(Color.class);
    Assert.assertSame(typeConverter.type, Color.class);
    Assert.assertEquals(Color.RED, typeConverter.fromInteger(0));
    Assert.assertTrue(typeConverter.toInteger(Color.GREEN) == 1);
  }

  @Test public void longTest() throws Exception {
    final LongTypeConverter typeConverter = LongClass.TYPE_CONVERTER;
    Assert.assertSame(typeConverter.type, LongClass.class);
    Assert.assertTrue(((LongClass)typeConverter.fromLong(1234567890123456789L)).value == 1234567890123456789L);
    Assert.assertTrue(typeConverter.toLong(new LongClass(-8765432109876543210L)) == -8765432109876543210L);
  }

  @Test public void string() throws Exception {
    StringTypeConverter typeConverter;
    typeConverter = StringTypeConverter.BIG_DECIMAL;
    Assert.assertSame(typeConverter.type, BigDecimal.class);
    Assert.assertEquals(new BigDecimal("1.23"), typeConverter.fromString("1.23"));
    Assert.assertEquals("1.23", typeConverter.toString(new BigDecimal("1.23")));
    typeConverter = StringTypeConverter.BIG_INTEGER;
    Assert.assertSame(typeConverter.type, BigInteger.class);
    Assert.assertEquals(new BigInteger("123"), typeConverter.fromString("123"));
    Assert.assertEquals("123", typeConverter.toString(new BigInteger("123")));
    typeConverter = StringTypeConverter.forEnum(Color.class);
    Assert.assertSame(typeConverter.type, Color.class);
    Assert.assertEquals(Color.RED, typeConverter.fromString("RED"));
    Assert.assertEquals("GREEN", typeConverter.toString(Color.GREEN));
  }

}
