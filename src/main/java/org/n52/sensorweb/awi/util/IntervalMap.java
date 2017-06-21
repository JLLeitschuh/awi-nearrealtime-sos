package org.n52.sensorweb.awi.util;

import java.util.Optional;
import java.util.Set;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface IntervalMap<K, V> {
    Optional<V> get(K lower, K upper);

    default V getOrDefault(K lower, K upper, V value) {
        return get(lower, upper).orElse(value);
    }

    default Optional<V> get(K key) {
        return get(key, key);
    }

    default V getOrDefault(K key, V value) {
        return get(key).orElse(value);
    }

    Set<V> search(K lower, K upper);

    default Set<V> search(K key) {
        return search(key, key);
    }

}
