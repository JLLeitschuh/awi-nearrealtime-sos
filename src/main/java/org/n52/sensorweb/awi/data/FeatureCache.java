package org.n52.sensorweb.awi.data;

import java.util.Set;

import javax.annotation.Nonnull;

import org.joda.time.DateTime;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface FeatureCache {
    @Nonnull
    String getFeatureId(@Nonnull String platform, @Nonnull DateTime time);

    @Nonnull
    Set<String> getFeatureIds(@Nonnull String platform);
}
