/*
 * Copyright 2016 52°North GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.n52.sensorweb.awi.sos;

import static java.util.stream.Collectors.toSet;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toDisjunction;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.janmayen.exception.CompositeException;
import org.n52.janmayen.function.Functions;
import org.n52.janmayen.function.Predicates;
import org.n52.janmayen.function.ThrowingBiFunction;
import org.n52.janmayen.stream.MoreCollectors;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.ExpeditionGeometry;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sos.ds.hibernate.util.DefaultResultTransfomer;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.gml.AbstractFeature;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.gml.ReferenceType;
import org.n52.shetland.ogc.om.NamedValue;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.features.FeatureCollection;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.om.values.TimeRangeValue;
import org.n52.shetland.ogc.ows.exception.InvalidParameterValueException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.request.GetFeatureOfInterestRequest;
import org.n52.shetland.ogc.sos.response.GetFeatureOfInterestResponse;
import org.n52.shetland.ogc.swe.RangeValue;
import org.n52.sos.cache.SosContentCache;
import org.n52.sos.ds.AbstractGetFeatureOfInterestHandler;
import org.n52.sos.ds.hibernate.util.SpatialRestrictions;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Handler for {@code GetFeatureOfInterest} requests.
 *
 * @author Christian Autermann
 */
public class AWIGetFeatureOfInterestHandler extends AbstractGetFeatureOfInterestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AWIGetFeatureOfInterestHandler.class);
    private final SessionFactory sessionFactory;

    /**
     * Creates a new handler.
     *
     * @param sessionFactory the session factory
     */
    @Inject
    public AWIGetFeatureOfInterestHandler(SessionFactory sessionFactory) {
        super(SosConstants.SOS);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public GetFeatureOfInterestResponse getFeatureOfInterest(GetFeatureOfInterestRequest request)
            throws OwsExceptionReport {

        ObservationFilter filter = ObservationFilter.builder()
                .setFeatures(request.getFeatureIdentifiers())
                .setProperties(request.getObservedProperties())
                .setProcedures(request.getProcedures())
                .setSpatialFilter(request.getSpatialFilters())
                .build();

        return new GetFeatureOfInterestResponse(request.getService(), request.getVersion(), getFeatures(filter));
    }

    /**
     * Get the feature collection matching the supplied filters.
     *
     * @param filter the filters
     *
     * @return the feature collection
     *
     * @throws OwsExceptionReport if one of the filters is not supported
     */
    private FeatureCollection getFeatures(ObservationFilter filter) throws OwsExceptionReport {
        Session session = sessionFactory.openSession();
        try {

            Set<String> identifiers = getFeatureIdentifiers(filter, session);

            if (identifiers.isEmpty()) {
                return new FeatureCollection();
            }

            LOG.debug("Querying features: {}", identifiers);

            Criteria stationary = session.createCriteria(Platform.class)
                    .setComment("Getting stationary features")
                    .setProjection(Projections.projectionList()
                            .add(Projections.property(Platform.CODE))
                            .add(Projections.property(Platform.GEOMETRY)))
                    .add(Restrictions.in(Platform.CODE, identifiers));

            Criteria mobile = session.createCriteria(ExpeditionGeometry.class)
                    .setComment("Getting mobile features")
                    .setProjection(Projections.projectionList()
                            .add(Projections.property(ExpeditionGeometry.NAME))
                            .add(Projections.property(ExpeditionGeometry.GEOMETRY))
                            .add(Projections.property(ExpeditionGeometry.BEGIN))
                            .add(Projections.property(ExpeditionGeometry.END)))
                    .add(Restrictions.in(ExpeditionGeometry.NAME, identifiers));

            DefaultResultTransfomer<SamplingFeature> transformer = tuple -> {
                SamplingFeature feature = createSamplingFeature((String) tuple[0], (Geometry) tuple[1]);
                if (tuple.length > 2) {
                    feature.addParameter(createSamplingTimeParameter((Date) tuple[2], (Date) tuple[3]));
                }
                return feature;
            };

            return Stream.of(stationary, mobile)
                    .map(Functions.currySecond(Criteria::setReadOnly, true))
                    .map(Functions.currySecond(Criteria::setResultTransformer, transformer))
                    .map(Criteria::list)
                    .flatMap(List<AbstractFeature>::stream)
                    .collect(toFeatureCollection());

        } catch (HibernateException e) {
            throw new NoApplicableCodeException().causedBy(e);
        } finally {
            session.close();
        }
    }

    /**
     * Create a {@link SamplingFeature} for the supplied identifier and geometry.
     *
     * @param identifier the feature identifier
     * @param geometry   the geometry
     *
     * @return the sampling feature
     */
    private SamplingFeature createSamplingFeature(String identifier, Geometry geometry) {
        SamplingFeature feature = new SamplingFeature(new CodeWithAuthority(identifier));
        feature.setGeometry(geometry);
        return feature;
    }

    /**
     * Create a feature parameter containing the sampling time.
     *
     * @param beginDate the begin date
     * @param endDate   the end date
     *
     * @return the parameter
     */
    private NamedValue<RangeValue<DateTime>> createSamplingTimeParameter(Date beginDate, Date endDate) {
        ReferenceType name = new ReferenceType(OmConstants.PHEN_SAMPLING_TIME);
        DateTime begin = new DateTime(beginDate).withZoneRetainFields(DateTimeZone.UTC);
        DateTime end = new DateTime(endDate).withZoneRetainFields(DateTimeZone.UTC).plusDays(1).minusMillis(1);
        return new NamedValue<>(name, new TimeRangeValue(new RangeValue<>(begin, end), OmConstants.PHEN_UOM_ISO8601));
    }

    /**
     * Get the identifiers of the features matching the requested filters.
     *
     * @param filter  the filters
     * @param session the session
     *
     * @return the feature identifiers
     *
     * @throws OwsExceptionReport if the supplied filters are not supported
     */
    private Set<String> getFeatureIdentifiers(ObservationFilter filter, Session session)
            throws OwsExceptionReport {
        SosContentCache cache = getCache();

        // begin with the list of feature identifiers if present or else all identifiers known to the service
        Set<String> identifiers = new HashSet<>(Optional.of(filter.getFeatures())
                .filter(Predicates.not(Set::isEmpty))
                .orElseGet(cache::getFeaturesOfInterest));

        // get the procedures offering the requested observable properties and get their respective features
        Optional.of(filter.getProperties())
                .filter(Predicates.not(Set::isEmpty))
                .map(properties -> properties.stream()
                    .map(cache::getProceduresForObservableProperty).flatMap(Set::stream)
                    .map(cache::getRelatedFeaturesForOffering).flatMap(Set::stream)
                    .collect(toSet()))
                .ifPresent(identifiers::retainAll);

        // get the features for the requested procedures
        Optional.of(filter.getProcedures())
                .filter(Predicates.not(Set::isEmpty))
                .map(procedures -> procedures.stream()
                    .map(cache::getRelatedFeaturesForOffering).flatMap(Set::stream)
                    .collect(toSet()))
                .ifPresent(identifiers::retainAll);

        // get the identifiers for the spatial filters
        CompositeException errors = new CompositeException();

        ThrowingBiFunction<Set<SpatialFilter>, Session, Set<String>, OwsExceptionReport> getFeatureIdentifiers
                = this::getFeatureIdentifiers;

        Optional.of(filter.getSpatialFilters())
                .filter(Predicates.not(Set::isEmpty))
                .flatMap(errors.wrapFunction(getFeatureIdentifiers.currySecond(session)))
                .ifPresent(identifiers::retainAll);

        errors.throwCause(OwsExceptionReport.class);

        return identifiers;
    }

    /**
     * Get the identifiers matching the spatial filters.
     *
     * @param filters the spatial filters
     * @param session the session
     *
     * @return the feature identifiers
     *
     * @throws OwsExceptionReport if one of the supplied filter operators is not supported
     */
    private Set<String> getFeatureIdentifiers(Set<SpatialFilter> filters, Session session) throws OwsExceptionReport {
        QueryContext ctx = QueryContext.forSensor();

        ThrowingBiFunction<String, SpatialFilter, Criterion, OwsExceptionReport> getSpatialFilter
                = SpatialRestrictions::filter;

        CompositeException errors = new CompositeException();

        Criteria stationary = session.createCriteria(Sensor.class)
                .setComment("Getting stationary feature ids for spatial filter")
                .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                .setProjection(Projections.property(ctx.getPlatformPath(Platform.CODE)))
                .add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true))
                .add(filters.stream()
                        .map(errors.wrapFunction(getSpatialFilter.curryFirst(ctx.getPlatformPath(Platform.GEOMETRY))))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toDisjunction()));

        DetachedCriteria validPlatforms = DetachedCriteria.forClass(Sensor.class)
                .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                .setProjection(Projections.property(ctx.getPlatformPath(Platform.CODE)))
                .add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true));

        Criteria mobile = session.createCriteria(ExpeditionGeometry.class)
                .setComment("Getting mobile feature ids for spatial filter")
                .setProjection(Projections.property(ExpeditionGeometry.NAME))
                .add(Subqueries.propertyIn(ExpeditionGeometry.PLATFORM, validPlatforms))
                .add(filters.stream()
                        .map(errors.wrapFunction(getSpatialFilter.curryFirst(ctx
                                .getPlatformPath(ExpeditionGeometry.GEOMETRY))))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(toDisjunction()));

        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.spatialFilter).causedBy(e));

        DefaultResultTransfomer<String> transformer = tuple -> (String) tuple[0];

        return Stream.of(stationary, mobile)
                .map(Functions.currySecond(Criteria::setReadOnly, true))
                .map(Functions.currySecond(Criteria::setResultTransformer, transformer))
                .map(Criteria::list)
                .flatMap(List<String>::stream)
                .collect(toSet());
    }

    /**
     * Returns a {@code Collector} that accumulates {@linkplain AbstractFeature features} to a
     * {@link FeatureCollection}.
     *
     * @return the collector
     */
    private static Collector<AbstractFeature, ?, FeatureCollection> toFeatureCollection() {
        Supplier<FeatureCollection> supplier = FeatureCollection::new;
        BiConsumer<FeatureCollection, AbstractFeature> accumulator = FeatureCollection::addMember;
        BiConsumer<FeatureCollection, FeatureCollection> combiner = (a, b) -> a.setMembers(b.getMembers());
        return MoreCollectors.collector(supplier, accumulator, combiner);
    }
}
