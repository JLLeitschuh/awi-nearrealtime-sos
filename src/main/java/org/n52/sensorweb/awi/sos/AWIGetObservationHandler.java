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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.joda.time.DateTime;

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
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosProcedureDescriptionUnknownType;
import org.n52.shetland.ogc.sos.exception.ResponseExceedsSizeLimitException;
import org.n52.shetland.ogc.sos.request.GetObservationRequest;
import org.n52.shetland.ogc.sos.response.GetObservationResponse;
import org.n52.shetland.ogc.swe.SweConstants;
import org.n52.sos.cache.SosContentCache;
import org.n52.sos.ds.AbstractGetObservationHandler;
import org.n52.sos.exception.ows.concrete.UnsupportedOperatorException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetObservationHandler extends AbstractGetObservationHandler {

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), 4326);

    public AWIGetObservationHandler() {
        super(SosConstants.SOS);
    }

    @Override
    public GetObservationResponse getObservation(GetObservationRequest request) throws OwsExceptionReport {
        String service = request.getService();
        String version = request.getVersion();

        Set<String> properties = new HashSet<>(Optional.ofNullable(request.getObservedProperties()).orElseGet(Collections::emptyList));
        Set<String> offerings = new HashSet<>(Optional.ofNullable(request.getOfferings()).orElseGet(Collections::emptyList));
        Set<String> procedures =new HashSet<>( Optional.ofNullable(request.getProcedures()).orElseGet(Collections::emptyList));
        Set<String> features =new HashSet<>( Optional.ofNullable(request.getFeatureIdentifiers()).orElseGet(Collections::emptyList));
        Set<TemporalFilter> temporalFilters = new HashSet<>(Optional.ofNullable(request.getTemporalFilters()).orElseGet(Collections::emptyList));
        Optional<SpatialFilter> spatialFilter = Optional.ofNullable(request.getSpatialFilter());

        if (spatialFilter.isPresent() && spatialFilter.get().getOperator() != SpatialOperator.BBOX) {
            throw new UnsupportedOperatorException(spatialFilter.get().getOperator());
        }

        if (properties.isEmpty() && offerings.isEmpty() &&
            procedures.isEmpty() && features.isEmpty() &&
            temporalFilters.isEmpty() && !spatialFilter.isPresent()) {
            throw new ResponseExceedsSizeLimitException();
        }


        SosContentCache cache = getCache();



        String phenomenon = null;
        String unit = null;
        String procedure = null;
        String feature = null;
        DateTime time = null;

        double value = 0;
        double latitude = 0;
        double longitude = 0;

        OmObservableProperty observableProperty = new OmObservableProperty(phenomenon, null, unit, SweConstants.VT_QUANTITY);

        OmObservationConstellation observationConstellation = new OmObservationConstellation();
        observationConstellation.setObservationType(OmConstants.OBS_TYPE_MEASUREMENT);
        observationConstellation.setFeatureOfInterest(new SamplingFeature(new CodeWithAuthority(feature)));
        observationConstellation.setObservableProperty(observableProperty);
        observationConstellation.setProcedure(new SosProcedureDescriptionUnknownType(procedure));

        TimeInstant phenomenonTime = new TimeInstant(time);
        Value<Double>  quantityValue = new QuantityValue(value, observableProperty.getUnit());
        NamedValue<Geometry> spatialFilteringParameter
                = new NamedValue<>(new ReferenceType(Sos2Constants.HREF_PARAMETER_SPATIAL_FILTERING_PROFILE),
                                   new GeometryValue(geometryFactory.createPoint(new Coordinate(longitude, latitude))));


        OmObservation observation = new OmObservation();
        observation.setObservationConstellation(observationConstellation);
        observation.setResultTime(phenomenonTime);
        observation.setValue(new SingleObservationValue<>(phenomenonTime, quantityValue));
        observation.addParameter(spatialFilteringParameter);


        Map<String, Set<String>> hierarchy = new HashMap<>(procedures.size());



        GetObservationResponse response = new GetObservationResponse();
        response.setService(service);
        response.setVersion(version);


        return response;
    }



    private NamedValue<Geometry> createSpatialFilteringProfileParameter(double longitude, double latitude) {
        return new NamedValue<>(new ReferenceType(Sos2Constants.HREF_PARAMETER_SPATIAL_FILTERING_PROFILE),
                                new GeometryValue(geometryFactory.createPoint(new Coordinate(longitude, latitude))));
    }

}
