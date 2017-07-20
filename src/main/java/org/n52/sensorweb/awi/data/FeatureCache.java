package org.n52.sensorweb.awi.data;

import java.util.Set;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;

/**
 * A simple cache to map from platforms to feature identifiers.
 *
 * @author Christian Autermann
 */
public interface FeatureCache {
    /**
     * Get the feature identifier for the specified platform and time.
     *
     * @param platform the platform identifier
     * @param time     the time
     *
     * @return the feature identifier
     */
    @Nonnull
    String getFeatureId(@Nonnull String platform, @Nonnull DateTime time);

    /**
     * Get all feature identifiers of a platform.
     *
     * @param platform the platform identifier
     *
     * @return the feature identifiers
     */
    @Nonnull
    Set<String> getFeatureIds(@Nonnull String platform);
}
