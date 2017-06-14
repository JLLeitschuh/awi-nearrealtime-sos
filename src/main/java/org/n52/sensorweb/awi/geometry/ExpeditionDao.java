package org.n52.sensorweb.awi.geometry;

import java.util.Optional;
import java.util.Set;

import org.joda.time.DateTime;


/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface ExpeditionDao {
    Optional<String> getFeatureId(String platform, DateTime time);

    Set<String> getFeatureIds(String platform);

}
