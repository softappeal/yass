package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.TagUtil;
import org.junit.Assert;
import org.junit.Test;

@Tag(123) public class TagUtilTest {

    @Test public void hasTag() {
        Assert.assertTrue(TagUtil.getTag(TagUtilTest.class) == 123);
        try {
            TagUtil.getTag(TagUtil.class);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("missing tag for 'class ch.softappeal.yass.util.TagUtil'", e.getMessage());
        }
    }

}
