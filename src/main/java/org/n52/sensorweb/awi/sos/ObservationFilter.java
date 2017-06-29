package org.n52.sensorweb.awi.sos;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.n52.janmayen.function.Predicates;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;


/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class ObservationFilter {

    private final Set<String> procedures;
    private final Set<String> features;
    private final Set<String> offerings;
    private final Set<String> properties;
    private final Set<TemporalFilter> temporalFilters;
    private final Set<SpatialFilter> spatialFilters;

    private ObservationFilter(Set<String> procedures, Set<String> features, Set<String> offerings,
                              Set<String> properties, Set<TemporalFilter> temporalFilters,
                              Set<SpatialFilter> spatialFilters) {
        this.procedures = requireNonNull(procedures);
        this.features = requireNonNull(features);
        this.offerings = requireNonNull(offerings);
        this.properties = requireNonNull(properties);
        this.temporalFilters = requireNonNull(temporalFilters);
        this.spatialFilters = requireNonNull(spatialFilters);
    }

    public Set<String> getProcedures() {
        return Collections.unmodifiableSet(this.procedures);
    }

    public Set<String> getFeatures() {
        return Collections.unmodifiableSet(this.features);
    }

    public Set<String> getOfferings() {
        return Collections.unmodifiableSet(this.offerings);
    }

    public Set<String> getProperties() {
        return Collections.unmodifiableSet(this.properties);
    }

    public Set<TemporalFilter> getTemporalFilters() {
        return Collections.unmodifiableSet(this.temporalFilters);
    }

    public Set<SpatialFilter> getSpatialFilters() {
        return Collections.unmodifiableSet(this.spatialFilters);
    }

    public boolean hasFilters() {
        return Stream.<Set<?>>of(this.procedures, this.offerings, this.properties,
                                 this.features, this.temporalFilters, this.spatialFilters)
                .anyMatch(Predicates.not(Set<?>::isEmpty));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Set<String> procedures = Collections.emptySet();
        private Set<String> features = Collections.emptySet();
        private Set<String> offerings = Collections.emptySet();
        private Set<String> properties = Collections.emptySet();
        private Set<TemporalFilter> temporalFilters = Collections.emptySet();
        private Set<SpatialFilter> spatialFilter = Collections.emptySet();

        private Builder() {
        }

        public Builder setProcedures(Collection<String> procedures) {
            this.procedures = asSet(procedures);
            return this;
        }

        public Builder setFeatures(Collection<String> features) {
            this.features = asSet(features);
            return this;
        }

        public Builder setOfferings(Collection<String> offerings) {
            this.offerings = asSet(offerings);
            return this;
        }

        public Builder setProperties(Collection<String> properties) {
            this.properties = asSet(properties);
            return this;
        }

        public Builder setTemporalFilters(Collection<TemporalFilter> temporalFilters) {
            this.temporalFilters = asSet(temporalFilters);
            return this;
        }

        public Builder setSpatialFilter(SpatialFilter spatialFilter) {
            this.spatialFilter = Optional.ofNullable(spatialFilter)
                    .map(Collections::singleton)
                    .orElseGet(Collections::emptySet);
            return this;
        }

        public Builder setSpatialFilter(Collection<SpatialFilter> spatialFilter) {
            this.spatialFilter = asSet(spatialFilter);
            return this;
        }

        public ObservationFilter build() {
            return new ObservationFilter(procedures, features, offerings, properties, temporalFilters, spatialFilter);
        }

        private static <T> Set<T> asSet(Collection<T> collection) {
            return Optional.ofNullable(collection)
                    .<Set<T>>map(HashSet::new)
                    .orElseGet(Collections::emptySet);
        }
    }

}
