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

import static java.util.stream.Collectors.toList;

import java.util.List;

import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.joda.time.DateTime;

import org.n52.sensorweb.awi.data.FeatureCache;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
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

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetObservationHandler extends AbstractGetObservationHandler {
    private final SessionFactory sessionFactory;
    private final FeatureCache featureCache;

    @Inject
    public AWIGetObservationHandler(FeatureCache expeditionDao, SessionFactory sessionFactory) {
        super(SosConstants.SOS);
        this.sessionFactory = sessionFactory;
        this.featureCache = expeditionDao;
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
        List<OmObservation> observations;

        Session session = sessionFactory.openSession();
        try {
            observations = getData(session, filter);
        } catch (HibernateException e) {
            throw new NoApplicableCodeException().causedBy(e);
        } finally {
            session.close();
        }

        GetObservationResponse response = new GetObservationResponse();
        response.setService(service);
        response.setVersion(version);
        response.setObservationCollection(observations);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<OmObservation> getData(Session session, ObservationFilter filter) throws OwsExceptionReport {
        QueryContext ctx = QueryContext.forData();
        List<Data> list = session.createCriteria(Data.class)
                .setComment("Getting observations")
                .createAlias(Data.SENSOR, ctx.getSensor())
                .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                .add(filter.getFilterCriterion(getCache(), ctx))
                .add(ObservationFilter.getCommonCriteria(ctx))
                .setReadOnly(true)
                .list();
        return list.stream().map(this::createObservation).collect(toList());
    }

    private OmObservation createObservation(Data data) {
        Sensor sensor = data.getSensor();
        Device device = sensor.getDevice();
        Platform platform = device.getPlatform();
        String procedure = String.format("%s:%s", platform.getCode(), device.getCode());
        DateTime dateTime = new DateTime(data.getTime());
        TimeInstant phenomenonTime = new TimeInstant(dateTime);
        TimeInstant resultTime = phenomenonTime;
        OmObservation observation = new OmObservation();
        observation.setResultTime(resultTime);
        String feature = this.featureCache.getFeatureId(platform.getCode(), dateTime);


        if (platform.isMobile()) {
            GeometryValue parameterValue = new GeometryValue(data.getGeometry());
            ReferenceType parameterName = new ReferenceType(Sos2Constants.HREF_PARAMETER_SPATIAL_FILTERING_PROFILE);
            observation.addParameter(new NamedValue<>(parameterName, parameterValue));
        }

        SosProcedureDescriptionUnknownType procedureDescription = new SosProcedureDescriptionUnknownType(procedure);
        OmObservableProperty observableProperty = new OmObservableProperty(
                sensor.getCode(), sensor.getName(), sensor.getUnit(), SweConstants.VT_QUANTITY);
        SamplingFeature samplingFeature = new SamplingFeature(new CodeWithAuthority(feature));
        OmObservationConstellation observationConstellation = new OmObservationConstellation(
                procedureDescription, observableProperty, samplingFeature, OmConstants.OBS_TYPE_MEASUREMENT);
        observation.setObservationConstellation(observationConstellation);
        QuantityValue value = new QuantityValue(data.getValue(), sensor.getUnit());
        SingleObservationValue<Double> observationValue = new SingleObservationValue<>(phenomenonTime, value);
        observation.setValue(observationValue);
        return observation;
    }

}
