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
import static org.n52.janmayen.function.Functions.curryFirst;
import static org.n52.sos.ds.hibernate.util.HibernateCollectors.toDisjunction;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;

import org.n52.janmayen.exception.CompositeException;
import org.n52.janmayen.function.Predicates;
import org.n52.janmayen.function.ThrowingBiFunction;
import org.n52.sensorweb.awi.data.FeatureCache;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sensorweb.awi.util.hibernate.MoreRestrictions;
import org.n52.sensorweb.awi.util.hibernate.ScrollableObservationStream;
import org.n52.shetland.ogc.filter.SpatialFilter;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.gml.CodeType;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.gml.ReferenceType;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.om.NamedValue;
import org.n52.shetland.ogc.om.ObservationStream;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservableProperty;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.OmObservationConstellation;
import org.n52.shetland.ogc.om.SingleObservationValue;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.om.values.GeometryValue;
import org.n52.shetland.ogc.om.values.QuantityValue;
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
import org.n52.sos.ds.AbstractGetObservationHandler;
import org.n52.sos.ds.hibernate.util.SpatialRestrictions;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.exception.ows.concrete.UnsupportedValueReferenceException;

/**
 * Handler for {@code GetObservation} requests.
 *
 * @author Christian Autermann
 */
public class AWIGetObservationHandler extends AbstractGetObservationHandler {
    private final SessionFactory sessionFactory;
    private final FeatureCache featureCache;

    /**
     * Creates a new handler.
     *
     * @param featureCache   the feature cache
     * @param sessionFactory the session factory
     */
    @Inject
    public AWIGetObservationHandler(FeatureCache featureCache, SessionFactory sessionFactory) {
        super(SosConstants.SOS);
        this.sessionFactory = sessionFactory;
        this.featureCache = featureCache;
    }

    @Override
    public GetObservationResponse getObservation(GetObservationRequest request) throws OwsExceptionReport {
        String service = request.getService();
        String version = request.getVersion();

        if (request.getResponseFormat() == null ||
            !request.getResponseFormat().equals(OmConstants.NS_OM_2)) {
            throw new InvalidParameterValueException(SosConstants.GetObservationParams.responseFormat,
                                                     request.getResponseFormat());
        }

        ObservationFilter filter = ObservationFilter.builder()
                .setFeatures(request.getFeatureIdentifiers())
                .setOfferings(request.getOfferings())
                .setProcedures(request.getProcedures())
                .setProperties(request.getObservedProperties())
                .setSpatialFilter(request.getSpatialFilter())
                .setTemporalFilters(request.getTemporalFilters())
                .build();

        if (!filter.hasFilters()) {
            throw new ResponseExceedsSizeLimitException();
        }
        ObservationStream observations;

        Session session = sessionFactory.openSession();
        try {
            observations = getData(session, filter);
        } catch (HibernateException e) {
            // we are streaming, only close the session if an error occured
            session.close();
            throw new NoApplicableCodeException().causedBy(e);
        }

        GetObservationResponse response = new GetObservationResponse();
        response.setService(service);
        response.setVersion(version);
        response.setObservationCollection(observations);
        return response;
    }

    @SuppressWarnings("unchecked")
    private ObservationStream getData(Session session, ObservationFilter filter) throws OwsExceptionReport {
        QueryContext ctx = QueryContext.forData();

        Criteria criteria = session.createCriteria(Data.class)
                .setComment("Getting observations")
                .createAlias(Data.SENSOR, ctx.getSensor())
                .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                .add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true));

        if (!filter.getProcedures().isEmpty()) {
            criteria.add(getProcedureCriterion(filter.getProcedures(), ctx));
        }

        if (!filter.getOfferings().isEmpty()) {
            criteria.add(getProcedureCriterion(filter.getOfferings(), ctx));
        }

        if (!filter.getProperties().isEmpty()) {
            criteria.add(getObservedPropertyCriterion(filter.getProperties(), ctx));
        }

        if (!filter.getTemporalFilters().isEmpty()) {
            criteria.add(getTemporalFiltersCriterion(filter.getTemporalFilters(), ctx));
        }

        if (!filter.getSpatialFilters().isEmpty()) {
            criteria.add(getSpatialFiltersCriterion(filter.getSpatialFilters(), ctx));
        }

        if (!filter.getFeatures().isEmpty()) {
            criteria.add(getFeatureCriterion(session, filter.getFeatures(), ctx));
        }
        ScrollableResults results = criteria.setReadOnly(true).scroll(ScrollMode.FORWARD_ONLY);
        return new ScrollableObservationStream(results, session, r -> createObservation((Data) r.get()[0]));
    }

    /**
     * Get a criterion for the supplied temporal filters.
     *
     * @param filter the temporal filters
     * @param ctx    the query context
     *
     * @return the criterion
     *
     * @throws OwsExceptionReport if one of the operatos is not supported or the a value reference is invalid
     */
    private Disjunction getTemporalFiltersCriterion(Set<TemporalFilter> filter, QueryContext ctx) throws
            OwsExceptionReport {
        CompositeException errors = new CompositeException();
        Disjunction criterion
                = filter.stream()
                        .map(curryFirst(errors.wrapFunction(AWIGetObservationHandler::getTemporalFilterCriterion), ctx))
                        .filter(Optional::isPresent).map(Optional::get).collect(toDisjunction());
        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.temporalFilter).causedBy(e));
        return criterion;
    }

    /**
     * Get a criterion for the supplied spatial filters.
     *
     * @param filters the spatial filters
     * @param ctx     the query context
     *
     * @return the criterion
     *
     * @throws OwsExceptionReport if one of the operators is not supported
     */
    private Disjunction getSpatialFiltersCriterion(Set<SpatialFilter> filters, QueryContext ctx) throws
            OwsExceptionReport {
        CompositeException errors = new CompositeException();
        ThrowingBiFunction<String, SpatialFilter, Criterion, OwsExceptionReport> get = SpatialRestrictions::filter;
        Disjunction criterion = Stream.concat(
                filters.stream().map(errors.wrapFunction(get.curryFirst(ctx.getDataPath(Data.GEOMETRY)))),
                filters.stream().map(errors.wrapFunction(get.curryFirst(ctx.getPlatformPath(Platform.GEOMETRY)))))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toDisjunction());
        errors.throwIfNotEmpty(e -> new InvalidParameterValueException()
                .at(Sos2Constants.GetObservationParams.spatialFilter).causedBy(e));
        return criterion;
    }

    /**
     * Get a criterion for the supplied feature identifiers.
     *
     * @param session the session
     * @param filter  the feature identifiers
     * @param ctx     the query context
     *
     * @return the criterion
     */
    private Disjunction getFeatureCriterion(Session session, Set<String> filter, QueryContext ctx) {
        @SuppressWarnings("unchecked")
        List<Expedition> expeditions = session.createCriteria(Expedition.class)
                .setComment("Getting expedition times for observation feature filter")
                .add(Restrictions.in(Expedition.NAME, filter))
                .setReadOnly(true)
                .list();

        Disjunction disjunction = Restrictions.disjunction();
        Set<String> expeditionNames = expeditions.stream().map(Expedition::getName).collect(toSet());
        expeditions.stream().map(e -> Restrictions.and(
                Restrictions.eq(ctx.getPlatformPath(Platform.CODE), e.getPlatform()),
                Restrictions.between(ctx.getDataPath(Data.TIME), e.getBegin(), e.getEnd())))
                .forEach(disjunction::add);
        filter.stream()
                .filter(Predicates.not(expeditionNames::contains))
                .map(f -> Restrictions.eq(ctx.getPlatformPath(Platform.CODE), f))
                .forEach(disjunction::add);
        return disjunction;
    }

    /**
     * Get a criterion for the supplied temporal filter.
     *
     * @param ctx the query context
     * @param tf  the temporal filter
     *
     * @return the criterion
     *
     * @throws OwsExceptionReport if the operator is not supported
     */
    private static Criterion getTemporalFilterCriterion(QueryContext ctx, TemporalFilter tf) throws OwsExceptionReport {
        if (!tf.getValueReference().equals(TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE) &&
            !tf.getValueReference().equals(TemporalRestrictions.RESULT_TIME_VALUE_REFERENCE)) {
            throw new UnsupportedValueReferenceException(tf.getValueReference());
        }
        return TemporalRestrictions.filter(tf.getOperator(), ctx.getDataPath(Data.TIME), tf.getTime());
    }

    /**
     * Get a criterion for the supplied procedure identifiers.
     *
     * @param urns the procedure identifiers
     * @param ctx  the query context
     *
     * @return the criterion
     */
    private static Criterion getProcedureCriterion(Set<String> urns, QueryContext ctx) {
        Pattern pattern = Pattern.compile("^([^:]+:[^:]+)(?::(.+))?$");
        return urns.stream()
                .map(pattern::matcher)
                .filter(Matcher::matches)
                .map(x -> MoreRestrictions.and(
                Optional.of(Restrictions.eq(ctx.getPlatformPath(Platform.CODE), x.group(1))),
                Optional.ofNullable(x.group(2)).map(device -> Restrictions.eq(ctx.getDevicePath(Device.CODE), device))))
                .filter(Optional::isPresent).map(Optional::get).collect(toDisjunction());
    }

    /**
     * Get a criterion for the supplied procedure identifiers.
     *
     * @param filter the observed properties
     * @param ctx    the query context
     *
     * @return the criterion
     */
    private static Criterion getObservedPropertyCriterion(Set<String> filter, QueryContext ctx) {
        return Restrictions.in(ctx.getSensorPath(Sensor.CODE), filter);
    }

    /**
     * Create a O&amp;M Observation for the data object.
     *
     * @param data         the data object
     *
     * @return the observation
     */
    private OmObservation createObservation(Data data) {
        Sensor sensor = data.getSensor();
        Device device = sensor.getDevice();
        Platform platform = device.getPlatform();
        String procedureCode = String.format("%s:%s", platform.getCode(), device.getCode());
        String procedureName = String.format("%s - %s", platform.getName(), device.getName());
        DateTime dateTime = new DateTime(data.getTime());
        TimeInstant phenomenonTime = new TimeInstant(dateTime);
        TimeInstant resultTime = phenomenonTime;

        String feature = featureCache.getFeatureId(platform.getCode(), dateTime);

        SosProcedureDescriptionUnknownType procedureDescription
                = new SosProcedureDescriptionUnknownType(procedureCode);
        procedureDescription.setName(new CodeType(procedureName));

        OmObservableProperty observableProperty = new OmObservableProperty(sensor.getCode());
        observableProperty.setName(new CodeType(sensor.getName()));
        observableProperty.setUnit(sensor.getUnit());
        observableProperty.setValueType(SweConstants.VT_QUANTITY);

        SamplingFeature samplingFeature = new SamplingFeature(new CodeWithAuthority(feature));
        samplingFeature.setName(new CodeType(feature));

        OmObservationConstellation observationConstellation = new OmObservationConstellation();
        observationConstellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
        observationConstellation.setProcedure(procedureDescription);
        observationConstellation.setFeatureOfInterest(samplingFeature);
        observationConstellation.setObservableProperty(observableProperty);

        QuantityValue value = new QuantityValue(data.getValue(), sensor.getUnit());
        SingleObservationValue<Double> observationValue = new SingleObservationValue<>(phenomenonTime, value);

        OmObservation observation = new OmObservation();
        observation.setResultTime(resultTime);
        observation.setObservationConstellation(observationConstellation);
        observation.setValue(observationValue);

        if (platform.isMobile()) {
            GeometryValue parameterValue = new GeometryValue(data.getGeometry());
            ReferenceType parameterName = new ReferenceType(Sos2Constants.HREF_PARAMETER_SPATIAL_FILTERING_PROFILE);
            observation.addParameter(new NamedValue<>(parameterName, parameterValue));
        }

        return observation;
    }

}
