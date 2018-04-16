package ch.softappeal.yass.test;

import ch.softappeal.yass.Tag;
import ch.softappeal.yass.Tags;
import org.junit.Assert;
import org.junit.Test;

@Tag(123) public class TagsTest {

    @Test public void hasTag() {
        Assert.assertTrue(Tags.getTag(TagsTest.class) == 123);
        try {
            Tags.getTag(Tags.class);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertEquals("missing tag for 'class ch.softappeal.yass.Tags'", e.getMessage());
        }
    }

}
