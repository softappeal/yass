package ch.softappeal.yass.util.test;

import ch.softappeal.yass.util.Tag;
import ch.softappeal.yass.util.Tags;
import org.junit.Assert;
import org.junit.Test;

@Tag(123) public class TagsTest {

    @Test public void hasTag() {
        Assert.assertTrue(Tags.getTag(TagsTest.class) == 123);
        try {
            Tags.getTag(Tags.class);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("missing tag for 'class ch.softappeal.yass.util.Tags'", e.getMessage());
        }
    }

}
