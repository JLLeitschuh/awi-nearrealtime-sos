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
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toConjunction;

import java.util.Collections;
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
import org.hibernate.criterion.Conjunction;
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
import org.n52.janmayen.stream.MoreCollectors;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.ExpeditionGeometry;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sensorweb.awi.util.hibernate.DefaultResultTransfomer;
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
import org.n52.sos.ds.AbstractGetFeatureOfInterestHandler;
import org.n52.sos.ds.hibernate.util.SpatialRestrictions;

import com.vividsolutions.jts.geom.Geometry;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetFeatureOfInterestHandler extends AbstractGetFeatureOfInterestHandler {
    private static final Logger LOG = LoggerFactory.getLogger(AWIGetFeatureOfInterestHandler.class);
    private final SessionFactory sessionFactory;

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

        GetFeatureOfInterestResponse response = new GetFeatureOfInterestResponse(request.getService(),
                                                                                 request.getVersion());
        response.setAbstractFeature(getFeatures(filter));
        return response;

    }

    private FeatureCollection getFeatures(ObservationFilter filter) throws OwsExceptionReport {
        Session session = sessionFactory.openSession();
        try {

            Set<String> identifiers = new HashSet<>(Optional.of(filter.getFeatures())
                    .filter(Predicates.not(Set::isEmpty))
                    .orElseGet(getCache()::getFeaturesOfInterest));


            Set<Set<String>> featuresFromProcedures = Optional.of(filter.getProcedures()).filter(Set::isEmpty)
                    .map(procedures -> procedures.stream()
                    .map(procedure -> getCache().getRelatedFeaturesForOffering(procedure)).collect(toSet()))
                    .orElseGet(Collections::emptySet);

            Set<Set<String>> proceduresFromProperties = Optional.of(filter.getProperties()).filter(Set::isEmpty)
                    .map(properties -> properties.stream()
                    .map(property -> getCache().getProceduresForObservableProperty(property))
                    .collect(toSet())).orElseGet(Collections::emptySet);

            if (proceduresFromProperties.stream().anyMatch(Set::isEmpty)) {
                identifiers.clear();
                LOG.debug("Clearing identifiers, no procedures from observable properties");
            } else {
                Set<Set<String>> featuresFromProceduresFromProperties
                        = proceduresFromProperties.stream().flatMap(Set::stream)
                                .map(procedure -> getCache().getRelatedFeaturesForOffering(procedure))
                                .collect(toSet());
                featuresFromProcedures.addAll(featuresFromProceduresFromProperties);
            }

            if (featuresFromProcedures.stream().anyMatch(Set::isEmpty)) {
                LOG.debug("Clearing identifiers, no features from procedures");
                identifiers.clear();
            } else {
                featuresFromProcedures.stream().forEach(identifiers::retainAll);
            }

            if (!filter.getSpatialFilters().isEmpty()) {
                CompositeException errors = new CompositeException();
                QueryContext ctx = QueryContext.forSensor();

                Conjunction platformFilter = filter.getSpatialFilters().stream().map(errors.<SpatialFilter, Criterion, OwsExceptionReport>wrap(sf ->
                    SpatialRestrictions.filter(ctx.getPlatformPath(Platform.GEOMETRY), sf)
                )).filter(Optional::isPresent).map(Optional::get).collect(toConjunction());
                Conjunction expeditionFilter = filter.getSpatialFilters().stream().map(errors.<SpatialFilter, Criterion, OwsExceptionReport>wrap(sf ->
                    SpatialRestrictions.filter(ExpeditionGeometry.GEOMETRY, sf)
                )).filter(Optional::isPresent).map(Optional::get).collect(toConjunction());

                errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                        .at(Sos2Constants.GetObservationParams.spatialFilter).causedBy(e));


                Criteria stationary = session.createCriteria(Sensor.class)
                        .setComment("Getting stationary feature ids for spatial filter")
                                .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                                .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                                .add(ObservationFilter.getCommonCriteria(ctx))
                                .setProjection(Projections.property(ctx.getPlatformPath(Platform.CODE)))
                                .add(platformFilter);

                Criteria mobile = session.createCriteria(ExpeditionGeometry.class)
                        .setComment("Getting mobile feature ids for spatial filter")
                                .setProjection(Projections.property(ctx.getExpeditionsPath(ExpeditionGeometry.NAME)))
                                .add(Subqueries.propertyIn(ExpeditionGeometry.PLATFORM, DetachedCriteria.forClass(Sensor.class)
                                        .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                                        .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                                        .setProjection(Projections.property(ctx.getPlatformPath(Platform.CODE)))
                                        .add(ObservationFilter.getCommonCriteria(ctx))))
                                .add(expeditionFilter);



                Set<String> featuresFromSpatialFilter
                        = Stream.of(stationary, mobile)
                                .map(Functions.currySecond(Criteria::setReadOnly, true))
                                .map(Criteria::list)
                                .flatMap(List<Object[]>::stream)
                                .map(x -> (String)x[0])
                                .collect(toSet());
                if (featuresFromSpatialFilter.isEmpty()) {
                    identifiers.clear();
                } else {
                    identifiers.retainAll(featuresFromSpatialFilter);
                }
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


            String uom = OmConstants.PHEN_UOM_ISO8601;
            ReferenceType samplingTimeName = new ReferenceType(OmConstants.PHEN_SAMPLING_TIME);

            DefaultResultTransfomer<SamplingFeature> transformer = tuple -> {
                SamplingFeature feature = new SamplingFeature(new CodeWithAuthority((String) tuple[0]));
                feature.setGeometry((Geometry) tuple[1]);
                if (tuple.length > 2) {
                    DateTime begin = new DateTime(tuple[2]).withZoneRetainFields(DateTimeZone.UTC);
                    DateTime end = new DateTime(tuple[3]).withZoneRetainFields(DateTimeZone.UTC).plusDays(1).minusMillis(1);
                    feature.addParameter(new NamedValue<>(samplingTimeName, new TimeRangeValue(new RangeValue<>(begin, end), uom)));
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

    private static Collector<AbstractFeature, ?, FeatureCollection> toFeatureCollection() {
        Supplier<FeatureCollection> supplier = FeatureCollection::new;
        BiConsumer<FeatureCollection, AbstractFeature> accumulator = FeatureCollection::addMember;
        BiConsumer<FeatureCollection, FeatureCollection> combiner = (a, b) -> a.setMembers(b.getMembers());
        return MoreCollectors.collector(supplier, accumulator, combiner);
    }
}
