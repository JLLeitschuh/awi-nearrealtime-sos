package org.n52.sensorweb.awi.sos;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.n52.janmayen.function.Functions.curryFirst;
import static org.n52.janmayen.stream.MoreCollectors.filtering;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toConjunction;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toDisjunction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;

import org.n52.janmayen.exception.CompositeException;
import org.n52.janmayen.function.ThrowingFunction;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.ows.exception.CompositeOwsException;
import org.n52.shetland.ogc.ows.exception.InvalidParameterValueException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.sos.cache.SosContentCache;
import org.n52.sos.ds.hibernate.util.SpatialRestrictions;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.exception.ows.concrete.UnsupportedValueReferenceException;

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
        this.procedures = Objects.requireNonNull(procedures);
        this.features = Objects.requireNonNull(features);
        this.offerings = Objects.requireNonNull(offerings);
        this.properties = Objects.requireNonNull(properties);
        this.temporalFilters = Objects.requireNonNull(temporalFilters);
        this.spatialFilters = Objects.requireNonNull(spatialFilters);
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

    public boolean isEmpty() {
        return this.properties.isEmpty() && this.offerings.isEmpty() && this.procedures.isEmpty() &&
               this.features.isEmpty() && this.temporalFilters.isEmpty() && this.spatialFilters.isEmpty();
    }

    private Criterion getTemporalFilterCriterion(String property, TemporalFilter tf) throws OwsExceptionReport {
        if (!tf.getValueReference().equals(TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE) &&
            !tf.getValueReference().equals(TemporalRestrictions.RESULT_TIME_VALUE_REFERENCE)) {
            throw new UnsupportedValueReferenceException(tf.getValueReference());
        }
        return TemporalRestrictions.filter(tf.getOperator(), property, tf.getTime());
    }

    public Criterion createTemporalFilterCriterion(QueryContext ctx) throws OwsExceptionReport {
        CompositeException errors = new CompositeException();
        String property = ctx.getDataPath(Data.TIME);
        Criterion criterion = getTemporalFilters().stream()
                .collect(groupingBy(TemporalFilter::getValueReference,
                                    mapping(curryFirst(errors.wrap(this::getTemporalFilterCriterion), property),
                                            filtering(Optional::isPresent, mapping(Optional::get, toDisjunction())))))
                .values().stream().collect(toConjunction());
        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.temporalFilter).causedBy(e));
        return criterion;
    }

    public Criterion getProcedureCriterion(QueryContext ctx) {
        return getProcedureCriterion(this.procedures, ctx);
    }

    public Criterion getOfferingCriterion(QueryContext ctx) {
        return getProcedureCriterion(this.offerings, ctx);
    }

    public Criterion getFeatureCriterion(QueryContext ctx) {
        String dataTime = ctx.getDataPath(Data.TIME);
        String expeditionName = ctx.getExpeditionPath(Expedition.NAME);
        String expeditionBegin = ctx.getExpeditionPath(Expedition.BEGIN);
        String expeditionEnd = ctx.getExpeditionPath(Expedition.END);
        String platformCode = ctx.getPlatformPath(Platform.CODE);
        String platformGeometry = ctx.getPlatformPath(Platform.GEOMETRY);
        return this.features.stream().map(x
                -> Restrictions.or(
                        // mobile
                        Restrictions.and(Restrictions.eq(expeditionName, x),
                                         Restrictions.geProperty(dataTime, expeditionBegin),
                                         Restrictions.leProperty(dataTime, expeditionEnd)),
                        // stationary
                        Restrictions.and(Restrictions.isNull(ctx.getExpedition()),
                                         Restrictions.isNotNull(platformGeometry),
                                         Restrictions.eq(platformCode, x)))
        ).collect(toDisjunction());
    }

    public Criterion isMobile(QueryContext ctx) {
        String dataTime = ctx.getDataPath(Data.TIME);
        String expeditionBegin = ctx.getExpeditionPath(Expedition.BEGIN);
        String expeditionEnd = ctx.getExpeditionPath(Expedition.END);
        return Restrictions.and(Restrictions.isNotNull(ctx.getExpedition()),
                                Restrictions.geProperty(dataTime, expeditionBegin),
                                Restrictions.leProperty(dataTime, expeditionEnd));
    }

    public Criterion isStationary(QueryContext ctx) {
        String platformGeometry = ctx.getPlatformPath(Platform.GEOMETRY);
        return Restrictions.and(Restrictions.isNull(ctx.getExpedition()),
                                Restrictions.isNotNull(platformGeometry));
    }

    public Criterion getObservedPropertiesCriterion(SosContentCache cache, QueryContext ctx) {
        if (this.properties.isEmpty()) {
            return Restrictions.conjunction();
        }
        String sensorCode = ctx.getSensorPath(Platform.CODE);
        return this.properties.stream().map(x
                -> Restrictions.and(Restrictions.eq(sensorCode, x),
                                    getProcedureCriterion(cache.getProceduresForObservableProperty(x), ctx))
        ).collect(toDisjunction());

    }

    private Criterion getProcedureCriterion(Set<String> urns, QueryContext ctx) {
        if (urns.isEmpty()) {
            return Restrictions.conjunction();
        } else {
            String deviceCode = ctx.getDevicePath(Device.CODE);
            String platformCode = ctx.getPlatformPath(Platform.CODE);
            Pattern pattern = Pattern.compile("^([^:]+:[^:]+)(?::(.+))?$");
            return urns.stream().map(pattern::matcher).filter(Matcher::matches).map(x -> {
                String platform = x.group(1);
                String device = x.group(2);
                if (device == null) {
                    return Restrictions.eq(platformCode, platform);
                } else {
                    return Restrictions.and(Restrictions.eq(platformCode, platform),
                                            Restrictions.eq(deviceCode, device));
                }
            }).collect(toDisjunction());
        }
    }

    public Criterion getFilterCriterion(SosContentCache cache, QueryContext ctx) throws OwsExceptionReport {
        Disjunction criteria = Restrictions.disjunction();
        criteria.add(getFeatureCriterion(ctx));
        criteria.add(getObservedPropertiesCriterion(cache, ctx));
        criteria.add(getOfferingCriterion(ctx));
        criteria.add(getProcedureCriterion(ctx));
        CompositeOwsException errors = new CompositeOwsException();
        try {
            criteria.add(createTemporalFilterCriterion(ctx));
        } catch (OwsExceptionReport e) {
            errors.add(e);
        }
        try {
            criteria.add(createSpatialFilterCriterion(ctx));
        } catch (OwsExceptionReport e) {
            errors.add(e);
        }
        errors.throwIfNotEmpty();
        return criteria;
    }

    public Criterion createSpatialFilterCriterion(QueryContext ctx) throws OwsExceptionReport {
        CompositeException errors = new CompositeException();
        String platformGeometry = ctx.getPlatformPath(Platform.GEOMETRY);
        String dataGeometry = ctx.getDataPath(Data.GEOMETRY);
        Conjunction criterion = getSpatialFilters().stream().map(errors
                .wrap((ThrowingFunction<SpatialFilter, Criterion, OwsExceptionReport>) x
                        -> Restrictions.or(
                        Restrictions.and(Restrictions.isNull(platformGeometry),
                                         SpatialRestrictions.filter(dataGeometry, x)),
                        Restrictions.and(Restrictions.isNotNull(platformGeometry),
                                         SpatialRestrictions.filter(platformGeometry, x)))
                )).filter(Optional::isPresent).map(Optional::get).collect(toConjunction());
        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.spatialFilter).causedBy(e));
        return criterion;

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
            this.spatialFilter = Optional.ofNullable(spatialFilter).map(Collections::singleton)
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
