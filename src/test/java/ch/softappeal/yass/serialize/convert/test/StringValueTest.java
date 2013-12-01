package ch.softappeal.yass.serialize.convert.test;

import org.junit.Assert;
import org.junit.Test;

public class StringValueTest {

  @Test public void test() throws Exception {
    final String value = "2013-02-20";
    final DateTime dateTime = new DateTime(value);
    Assert.assertSame(dateTime.value, value);
    Assert.assertEquals(dateTime.toString(), value);
    Assert.assertSame(DateTime.TYPE_CONVERTER.type, DateTime.class);
    Assert.assertSame(value, ((DateTime)DateTime.TYPE_CONVERTER.fromString(value)).value);
    Assert.assertSame(dateTime.value, DateTime.TYPE_CONVERTER.toString(dateTime));
  }

}
