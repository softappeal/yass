package ch.softappeal.yass.serialize.test;

import ch.softappeal.yass.serialize.TypeConverter;
import ch.softappeal.yass.serialize.TypeConverters;
import ch.softappeal.yass.serialize.contract.Color;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TypeConvertersTest {

  @Test public void enumToInteger() throws Exception {
    final TypeConverter<Color, Integer> typeConverter = AbstractFastSerializer.enumToInteger(Color.class);
    Assert.assertSame(typeConverter.type, Color.class);
    Assert.assertSame(typeConverter.serializableType, Integer.class);
    Assert.assertSame(Color.RED, typeConverter.from(0));
    Assert.assertTrue(typeConverter.to(Color.GREEN) == 1);
  }

  @Test public void bigIntegerToString() throws Exception {
    Assert.assertSame(TypeConverters.BIGINTEGER_TO_STRING.type, BigInteger.class);
    Assert.assertSame(TypeConverters.BIGINTEGER_TO_STRING.serializableType, String.class);
    Assert.assertEquals(new BigInteger("123"), TypeConverters.BIGINTEGER_TO_STRING.from("123"));
    Assert.assertEquals("123", TypeConverters.BIGINTEGER_TO_STRING.to(new BigInteger("123")));
  }

  @Test public void bigDecimalToString() throws Exception {
    Assert.assertSame(TypeConverters.BIGDECIMAL_TO_STRING.type, BigDecimal.class);
    Assert.assertSame(TypeConverters.BIGDECIMAL_TO_STRING.serializableType, String.class);
    Assert.assertEquals(new BigDecimal("1.23"), TypeConverters.BIGDECIMAL_TO_STRING.from("1.23"));
    Assert.assertEquals("1.23", TypeConverters.BIGDECIMAL_TO_STRING.to(new BigDecimal("1.23")));
  }

}
