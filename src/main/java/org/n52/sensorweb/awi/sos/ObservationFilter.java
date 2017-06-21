package org.n52.sensorweb.awi.sos;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static org.n52.janmayen.function.Functions.curryFirst;
import static org.n52.janmayen.stream.MoreCollectors.filtering;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toConjunction;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toDisjunction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;

import org.n52.janmayen.exception.CompositeException;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
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
        return !this.properties.isEmpty() ||
               !this.offerings.isEmpty() ||
               !this.procedures.isEmpty() ||
               !this.features.isEmpty() ||
               !this.temporalFilters.isEmpty() ||
               !this.spatialFilters.isEmpty();
    }

    private Criterion getTemporalFilterCriterion(QueryContext ctx, TemporalFilter tf) throws OwsExceptionReport {
        if (!tf.getValueReference().equals(TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE) &&
            !tf.getValueReference().equals(TemporalRestrictions.RESULT_TIME_VALUE_REFERENCE)) {
            throw new UnsupportedValueReferenceException(tf.getValueReference());
        }
        return TemporalRestrictions.filter(tf.getOperator(), ctx.getDataPath(Data.TIME), tf.getTime());
    }

    public Optional<? extends Criterion> createTemporalFilterCriterion(QueryContext ctx) throws OwsExceptionReport {
        CompositeException errors = new CompositeException();
        Conjunction criterion = getTemporalFilters().stream()
                .collect(groupingBy(TemporalFilter::getValueReference,
                                    mapping(curryFirst(errors.wrap(this::getTemporalFilterCriterion), ctx),
                                            filtering(Optional::isPresent, mapping(Optional::get, toDisjunction())))))
                .values().stream().collect(toConjunction());
        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.temporalFilter).causedBy(e));
        return Optional.of(criterion).filter(ObservationFilter::hasConditions);
    }

    public Optional<? extends Criterion> getProcedureCriterion(QueryContext ctx) {
        return getProcedureCriterion(this.procedures, ctx);
    }

    public Optional<? extends Criterion> getOfferingCriterion(QueryContext ctx) {
        return getProcedureCriterion(this.offerings, ctx);
    }

    public Optional<? extends Criterion> getFeatureCriterion(QueryContext ctx) {
        return Optional.of(getFeatures().stream()
                .map(x -> getFeatureCriterion(ctx, x))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toDisjunction()))
                .filter(ObservationFilter::hasConditions);
    }

    public Optional<? extends Criterion> getObservedPropertiesCriterion(SosContentCache cache, QueryContext ctx) {
        return Optional.of(getProperties().stream()
                .map(property -> getObservedPropertyCriterion(cache, ctx, property))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toDisjunction()))
                .filter(ObservationFilter::hasConditions);
    }

    public Criterion getFilterCriterion(SosContentCache cache, QueryContext ctx) throws OwsExceptionReport {
        CompositeOwsException errors = new CompositeOwsException();
        Conjunction conjunction = Restrictions.conjunction();

        getFeatureCriterion(ctx).ifPresent(conjunction::add);
        getObservedPropertiesCriterion(cache, ctx).ifPresent(conjunction::add);
        getOfferingCriterion(ctx).ifPresent(conjunction::add);
        getProcedureCriterion(ctx).ifPresent(conjunction::add);

        try {
            createTemporalFilterCriterion(ctx).ifPresent(conjunction::add);
        } catch (OwsExceptionReport e) {
            errors.add(e);
        }
        try {
            createSpatialFilterCriterion(ctx).ifPresent(conjunction::add);
        } catch (OwsExceptionReport e) {
            errors.add(e);
        }
        errors.throwIfNotEmpty();
        return conjunction;
    }

    public Optional<? extends Criterion> createSpatialFilterCriterion(QueryContext ctx) throws OwsExceptionReport {
        CompositeException errors = new CompositeException();
        Conjunction criterion = getSpatialFilters().stream()
                .map(errors.<SpatialFilter, Criterion, OwsExceptionReport>wrap(x -> getSpatialFilterCriterion(ctx, x)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toConjunction());
        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.spatialFilter).causedBy(e));
        return Optional.of(criterion).filter(ObservationFilter::hasConditions);

    }

    private Criterion getSpatialFilterCriterion(QueryContext ctx, SpatialFilter x) throws OwsExceptionReport {
        return Restrictions.or(Restrictions.and(Restrictions.isNull(ctx.getPlatformPath(Platform.GEOMETRY)),
                                                SpatialRestrictions.filter(ctx.getDataPath(Data.GEOMETRY), x)),
                               Restrictions.and(Restrictions.isNotNull(ctx.getPlatformPath(Platform.GEOMETRY)),
                                                SpatialRestrictions.filter(ctx.getPlatformPath(Platform.GEOMETRY), x)));
    }

    private static Optional<? extends Criterion> getFeatureCriterion(QueryContext ctx, String x) {
        return Optional.of(Restrictions.or(
                Restrictions.and(
                        Restrictions.eq(ctx.getExpeditionsPath(Expedition.NAME), x),
                        Restrictions.geProperty(ctx.getDataPath(Data.TIME), ctx.getExpeditionsPath(Expedition.BEGIN)),
                        Restrictions.leProperty(ctx.getDataPath(Data.TIME), ctx.getExpeditionsPath(Expedition.END))),
                Restrictions.and(
                        Restrictions.isEmpty(ctx.getPlatformPath(Platform.EXPEDITIONS)),
                        Restrictions.isNotNull(ctx.getPlatformPath(Platform.GEOMETRY)),
                        Restrictions.eq(ctx.getPlatformPath(Platform.CODE), x))));
    }

    private static Optional<? extends Criterion> getObservedPropertyCriterion(SosContentCache cache, QueryContext ctx,
                                                                              String property) {
        return and(Optional.of(Restrictions.eq(ctx.getSensorPath(Platform.CODE), property)),
                   getProcedureCriterion(cache.getProceduresForObservableProperty(property), ctx));
    }

    private static Optional<? extends Criterion> getProcedureCriterion(Set<String> urns, QueryContext ctx) {
        Pattern pattern = Pattern.compile("^([^:]+:[^:]+)(?::(.+))?$");
        return Optional.of(urns.stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(x -> and(Optional.of(Restrictions.eq(ctx.getPlatformPath(Platform.CODE), x.group(1))),
                              Optional.ofNullable(x.group(2)).map(device -> Restrictions.eq(ctx
                              .getDevicePath(Device.CODE), device))))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toDisjunction()))
                .filter(ObservationFilter::hasConditions);
    }

    public static Conjunction getCommonCriteria(QueryContext ctx) {
        return Restrictions.and(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)),
                                Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)),
                                Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)),
                                Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true));
    }

    public static Criterion isMobile(QueryContext ctx) {
//        String dataTime = ctx.getDataPath(Data.TIME);
//        String expeditionBegin = ctx.getExpeditionsPath(Expedition.BEGIN);
//        String expeditionEnd = ctx.getExpeditionsPath(Expedition.END);
//        String platformExpeditions = ctx.getPlatformPath(Platform.EXPEDITIONS);
        String platformGeometry = ctx.getPlatformPath(Platform.GEOMETRY);
        return Restrictions.and(Restrictions.isNull(platformGeometry));
        //Restrictions.isNotEmpty(platformExpeditions),
        //Restrictions.geProperty(dataTime, expeditionBegin),
        //Restrictions.leProperty(dataTime, expeditionEnd));
    }

    public static Criterion isStationary(QueryContext ctx) {
        return Restrictions.isNotNull(ctx.getPlatformPath(Platform.GEOMETRY));
        //return Restrictions.and(
        //Restrictions.isEmpty(ctx.getPlatformPath(Platform.EXPEDITIONS)),
        //              );
    }

    public static Builder builder() {
        return new Builder();
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private static Optional<? extends Criterion> and(Optional<? extends Criterion>... criteria) {
        Conjunction conjunction = Arrays.stream(criteria)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toConjunction());
        return Optional.of(conjunction).filter(ObservationFilter::hasConditions);
    }

    private static boolean hasConditions(Junction j) {
        return j.conditions().iterator().hasNext();
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
