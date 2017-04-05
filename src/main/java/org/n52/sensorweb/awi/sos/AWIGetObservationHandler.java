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

import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.n52.janmayen.stream.MoreCollectors.filtering;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toConjunction;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toDisjunction;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.Restrictions;

import org.n52.janmayen.exception.CompositeException;
import org.n52.janmayen.function.Predicates;
import org.n52.sensorweb.awi.data.PropertyPath;
import org.n52.sensorweb.awi.data.entities.DataView;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.shetland.ogc.filter.FilterConstants.SpatialOperator;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.gml.ReferenceType;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.om.NamedValue;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservableProperty;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.OmObservationConstellation;
import org.n52.shetland.ogc.om.SingleObservationValue;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.om.values.GeometryValue;
import org.n52.shetland.ogc.om.values.QuantityValue;
import org.n52.shetland.ogc.om.values.Value;
import org.n52.shetland.ogc.ows.exception.InvalidParameterValueException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosProcedureDescriptionUnknownType;
import org.n52.shetland.ogc.sos.exception.ResponseExceedsSizeLimitException;
import org.n52.shetland.ogc.sos.request.GetObservationRequest;
import org.n52.shetland.ogc.sos.response.GetObservationResponse;
import org.n52.shetland.ogc.swe.SweConstants;
import org.n52.shetland.util.CollectionHelper;
import org.n52.sos.cache.SosContentCache;
import org.n52.sos.ds.AbstractGetObservationHandler;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.exception.ows.concrete.UnsupportedOperatorException;
import org.n52.sos.exception.ows.concrete.UnsupportedValueReferenceException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetObservationHandler extends AbstractGetObservationHandler {

    private final GeometryFactory geometryFactory
            = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);
    private final SessionFactory sessionFactory;

    public AWIGetObservationHandler(SessionFactory sessionFactory) {
        super(SosConstants.SOS);
        this.sessionFactory = sessionFactory;
    }

    @Override
    public GetObservationResponse getObservation(GetObservationRequest request) throws OwsExceptionReport {
        String service = request.getService();
        String version = request.getVersion();

        Set<String> properties = asSet(request.getObservedProperties());
        Set<String> offerings = asSet(request.getOfferings());
        Set<String> procedures = asSet(request.getProcedures());
        Set<String> features = asSet(request.getFeatureIdentifiers());
        Set<TemporalFilter> temporalFilters = asSet(request.getTemporalFilters());
        Optional<SpatialFilter> spatialFilter = Optional.ofNullable(request.getSpatialFilter());

        if (properties.isEmpty() && offerings.isEmpty() &&
            procedures.isEmpty() && features.isEmpty() &&
            temporalFilters.isEmpty() && !spatialFilter.isPresent()) {
            throw new ResponseExceedsSizeLimitException();
        }

        if (request.getResponseFormat() == null ||
            !request.getResponseFormat().equals(OmConstants.NS_OM_2)) {
            throw new InvalidParameterValueException(SosConstants.GetObservationParams.responseFormat,
                                                     request.getResponseFormat());
        }

        //TODO fill in actual procedures for parent procedures
        List<OmObservation> observations
                = getData(procedures, features, offerings, properties, temporalFilters, spatialFilter).stream()
                        .map(this::createObservation)
                        .collect(toList());

        GetObservationResponse response = new GetObservationResponse();
        response.setService(service);
        response.setVersion(version);
        response.setObservationCollection(observations);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<DataView> getData(Set<String> procedures, Set<String> features, Set<String> offerings,
                                   Set<String> properties, Set<TemporalFilter> temporalFilters,
                                   Optional<SpatialFilter> spatialFilter) throws OwsExceptionReport {

        Criterion filters
                = getFilterCriterion(procedures, features, offerings, properties, temporalFilters, spatialFilter);

        Session session = sessionFactory.openSession();
        try {
            return session.createCriteria(DataView.class)
                    .createAlias(DataView.SENSOR, DataView.SENSOR)
                    .add(Restrictions.isNotNull(PropertyPath.of(DataView.SENSOR, Sensor.CODE)))
                    .add(filters)
                    .setReadOnly(true)
                    .list();
        } catch(HibernateException e) {
            throw new NoApplicableCodeException().causedBy(e);
        } finally {
            session.close();
        }
    }

    private Junction getFilterCriterion(Set<String> procedures, Set<String> features, Set<String> offerings,
                                        Set<String> properties, Set<TemporalFilter> temporalFilters,
                                        Optional<SpatialFilter> spatialFilter) throws OwsExceptionReport {
        CompositeException errors = new CompositeException();

        SosContentCache cache = getCache();
        Junction conjunction = Restrictions.conjunction();
        // features, procedures and offerings are all the same for this service
        List<Set<String>> nonEmptyProcedureFilters = Stream.of(procedures, features, offerings)
                .filter(Predicates.not(Set::isEmpty)).collect(toList());
        final Set<String> urns;
        // do we have any procedure filters?
        if (!nonEmptyProcedureFilters.isEmpty()) {
            // create the intersection of the filters
            Set<String> intersection = CollectionHelper.intersection(nonEmptyProcedureFilters);
            if (properties.isEmpty()) {
                // create URNs from the supplied observable properties
                urns = intersection.stream()
                        .flatMap(procedure -> cache.getObservablePropertiesForProcedure(procedure).stream()
                        .map(property -> String.format("%s:%s", procedure, property)))
                        .collect(toSet());
            } else {
                // create URNs from all cached observable properties
                urns = intersection.stream()
                        .flatMap(procedure -> properties.stream()
                        .filter(property -> cache.hasObservablePropertyForProcedure(procedure, property))
                        .map(property -> String.format("%s:%s", procedure, property)))
                        .collect(toSet());
            }
        } else if (!properties.isEmpty()) {
            // create the URNs from all procedures matching the observable properties
            urns = properties.stream()
                    .flatMap(property -> cache.getProceduresForObservableProperty(property).stream()
                    .map(procedure -> String.format("%s:%s", procedure, property)))
                    .collect(toSet());
        } else {
            urns = null;
        }

        if (urns != null) {
            conjunction.add(Restrictions.in(DataView.CODE, urns));
        }

        conjunction.add(temporalFilters.stream()
                .collect(Collectors.groupingBy(
                        TemporalFilter::getValueReference,
                        mapping(errors.wrap(this::getTemporalFilterCriterion),
                                filtering(Optional::isPresent, mapping(Optional::get, toDisjunction())))))
                .values().stream()
                .collect(toConjunction()));

        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.temporalFilter).causedBy(e));

        if (spatialFilter.isPresent()) {
            SpatialFilter filter = spatialFilter.get();

            if (filter.getOperator() != SpatialOperator.BBOX) {
                throw new UnsupportedOperatorException(filter.getOperator());
            }

            Geometry geometry = filter.getGeometry();
            Envelope envelope = geometry.getEnvelopeInternal();
            conjunction.add(Restrictions.and(Restrictions.ge(DataView.LATITUDE, envelope.getMinY()),
                                             Restrictions.le(DataView.LATITUDE, envelope.getMaxY()),
                                             Restrictions.ge(DataView.LONGITUDE, envelope.getMinX()),
                                             Restrictions.le(DataView.LONGITUDE, envelope.getMaxX())));
        }



        return conjunction;
    }

    private Criterion getTemporalFilterCriterion(TemporalFilter tf) throws OwsExceptionReport {
        if (!tf.getValueReference().equals(TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE) &&
            !tf.getValueReference().equals(TemporalRestrictions.RESULT_TIME_VALUE_REFERENCE)) {
            throw new UnsupportedValueReferenceException(tf.getValueReference());
        }
        return TemporalRestrictions.filter(tf.getOperator(), DataView.TIME, tf.getTime());
    }

    private OmObservation createObservation(DataView data) {
        String procedure = String.format("%s:%s:%s",
                                         data.getPlatform().getType(),
                                         data.getPlatform().getCode(),
                                         data.getDevice().getCode());
        String feature = procedure;

        OmObservationConstellation observationConstellation = new OmObservationConstellation(
                new SosProcedureDescriptionUnknownType(procedure),
                new OmObservableProperty(data.getSensor().getCode(),
                                         data.getSensor().getName(),
                                         data.getSensor().getUnit(),
                                         SweConstants.VT_QUANTITY),
                new SamplingFeature(new CodeWithAuthority(feature)),
                OmConstants.OBS_TYPE_MEASUREMENT);

        TimeInstant phenomenonTime = new TimeInstant(data.getTime());
        Value<Double> quantityValue = new QuantityValue(data.getValue(), data.getSensor().getUnit());
        NamedValue<Geometry> spatialFilteringParameter
                = new NamedValue<>(new ReferenceType(Sos2Constants.HREF_PARAMETER_SPATIAL_FILTERING_PROFILE),
                                   new GeometryValue(this.geometryFactory.createPoint(
                                           new Coordinate(data.getLongitude(),
                                                          data.getLatitude()))));
        OmObservation observation = new OmObservation();
        observation.setObservationConstellation(observationConstellation);
        observation.setResultTime(phenomenonTime);
        observation.setValue(new SingleObservationValue<>(phenomenonTime, quantityValue));
        observation.addParameter(spatialFilteringParameter);
        return observation;
    }

    private static <T> Set<T> asSet(List<T> list) {
        return new HashSet<>(Optional.ofNullable(list).orElseGet(Collections::emptyList));
    }
}
