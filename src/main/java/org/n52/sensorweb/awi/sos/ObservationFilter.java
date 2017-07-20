package org.n52.sensorweb.awi.sos;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.n52.janmayen.function.Predicates;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;

/**
 * Aggregate of various filters.
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

    /**
     * Create a new {@code ObservationFilter}.
     *
     * @param procedures      the procedures
     * @param features        the features of interest
     * @param offerings       the offerings
     * @param properties      the observable properties
     * @param temporalFilters the temporal filters
     * @param spatialFilters  the spatial filters
     */
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

    /**
     * Get the procedures.
     *
     * @return the procedures
     */
    public Set<String> getProcedures() {
        return Collections.unmodifiableSet(this.procedures);
    }

    /**
     * Get the features of interest.
     *
     * @return the features of interest
     */
    public Set<String> getFeatures() {
        return Collections.unmodifiableSet(this.features);
    }

    /**
     * Get the offerings.
     *
     * @return the offerings
     */
    public Set<String> getOfferings() {
        return Collections.unmodifiableSet(this.offerings);
    }

    /**
     * Get the observable properties.
     *
     * @return the observable properties
     */
    public Set<String> getProperties() {
        return Collections.unmodifiableSet(this.properties);
    }

    /**
     * Get the temporal filters.
     *
     * @return the temporal filters
     */
    public Set<TemporalFilter> getTemporalFilters() {
        return Collections.unmodifiableSet(this.temporalFilters);
    }

    /**
     * Get the spatial filters.
     *
     * @return the spatial filters
     */
    public Set<SpatialFilter> getSpatialFilters() {
        return Collections.unmodifiableSet(this.spatialFilters);
    }

    /**
     * Checks if this filter has any restrictions.
     *
     * @return if this filter has any restrictions
     */
    public boolean hasFilters() {
        return Stream.<Set<?>>of(this.procedures, this.offerings, this.properties,
                                 this.features, this.temporalFilters, this.spatialFilters)
                .anyMatch(Predicates.not(Set<?>::isEmpty));
    }

    /**
     * Create a new {@link Builder}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@code ObservationFilter}.
     */
    public static class Builder {
        private Set<String> procedures = Collections.emptySet();
        private Set<String> features = Collections.emptySet();
        private Set<String> offerings = Collections.emptySet();
        private Set<String> properties = Collections.emptySet();
        private Set<TemporalFilter> temporalFilters = Collections.emptySet();
        private Set<SpatialFilter> spatialFilter = Collections.emptySet();

        /**
         * Creates a new {@code Builder}.
         */
        private Builder() {
        }

        /**
         * Sets the procedures.
         *
         * @param procedures the procedures
         *
         * @return {@code this}
         */
        public Builder setProcedures(@Nullable Collection<String> procedures) {
            this.procedures = asSet(procedures);
            return this;
        }

        /**
         * Sets the features of interest.
         *
         * @param features the features
         *
         * @return {@code this}
         */
        public Builder setFeatures(@Nullable Collection<String> features) {
            this.features = asSet(features);
            return this;
        }

        /**
         * Sets the offerings.
         *
         * @param offerings the offerings
         *
         * @return {@code this}
         */
        public Builder setOfferings(@Nullable Collection<String> offerings) {
            this.offerings = asSet(offerings);
            return this;
        }

        /**
         * Sets the observed properties.
         *
         * @param properties the observed properties
         *
         * @return {@code this}
         */
        public Builder setProperties(@Nullable Collection<String> properties) {
            this.properties = asSet(properties);
            return this;
        }

        /**
         * Sets the temporal filters.
         *
         * @param temporalFilters the temporal filters
         *
         * @return {@code this}
         */
        public Builder setTemporalFilters(@Nullable Collection<TemporalFilter> temporalFilters) {
            this.temporalFilters = asSet(temporalFilters);
            return this;
        }

        /**
         * Sets the spatial filter.
         *
         * @param spatialFilter the spatial filters
         *
         * @return {@code this}
         */
        public Builder setSpatialFilter(@Nullable SpatialFilter spatialFilter) {
            this.spatialFilter = Optional.ofNullable(spatialFilter)
                    .map(Collections::singleton)
                    .orElseGet(Collections::emptySet);
            return this;
        }

        /**
         * Sets the spatial filters-
         *
         * @param spatialFilter the spatial filters
         *
         * @return {@code this}
         */
        public Builder setSpatialFilter(Collection<SpatialFilter> spatialFilter) {
            this.spatialFilter = asSet(spatialFilter);
            return this;
        }

        /**
         * Create the filter.
         *
         * @return the observation filter
         */
        public ObservationFilter build() {
            return new ObservationFilter(procedures, features, offerings, properties, temporalFilters, spatialFilter);
        }

        private static <T> Set<T> asSet(@Nullable Collection<T> collection) {
            return Optional.ofNullable(collection)
                    .<Set<T>>map(HashSet::new)
                    .orElseGet(Collections::emptySet);
        }
    }

}
