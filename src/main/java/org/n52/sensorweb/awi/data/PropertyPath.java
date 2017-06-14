package org.n52.sensorweb.awi.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public final class PropertyPath {
    private PropertyPath() {
    }

    public static String of(String first, String... path) {
        return Stream.concat(Stream.of(first), Arrays.stream(path))
                .filter(Objects::nonNull).collect(Collectors.joining("."));
    }

}
