package ch.softappeal.yass.util;

import java.io.InputStream;

/**
 * @deprecated needed for backward compatibility
 */
@Deprecated
@FunctionalInterface public interface Resource {

    /**
     * @deprecated use {@link InputStreamSupplier}
     */
    @Deprecated
    InputStream create();

}
