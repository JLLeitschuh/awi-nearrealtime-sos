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

import javax.inject.Inject;

import org.n52.sensorweb.awi.sensor.SensorAPIClient;
import org.n52.shetland.ogc.ows.exception.InvalidParameterValueException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sensorML.SensorML20Constants;
import org.n52.shetland.ogc.sos.Sos2Constants.DescribeSensorParams;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosProcedureDescriptionUnknownType;
import org.n52.shetland.ogc.sos.request.DescribeSensorRequest;
import org.n52.shetland.ogc.sos.response.DescribeSensorResponse;
import org.n52.sos.ds.AbstractDescribeSensorHandler;

/**
 * {@code DescribeSensor} handler for the AWI Nearrealtime database.
 *
 * This handler simpley retrieves the sensor description using the <a href="https://sensor.awi.de/">AWI Sensor API</a>.
 *
 * @author Christian Autermann
 */
public class AWIDescribeSensorHandler extends AbstractDescribeSensorHandler {

    private final SensorAPIClient sensorApiClient;

    /**
     * Create a new {@code AWIDescribeSensorHandler}.
     *
     * @param sensorApiClient the Senor API Client
     */
    @Inject
    public AWIDescribeSensorHandler(SensorAPIClient sensorApiClient) {
        super(SosConstants.SOS);
        this.sensorApiClient = sensorApiClient;
    }

    @Override
    public DescribeSensorResponse getSensorDescription(DescribeSensorRequest request) throws OwsExceptionReport {
        checkFormat(request);
        checkValidTime(request);

        SosProcedureDescriptionUnknownType description = retrieveDescription(request);

        DescribeSensorResponse response = new DescribeSensorResponse(request.getService(), request.getVersion());
        response.setOutputFormat(SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL);
        response.addSensorDescription(description);
        return response;
    }

    /**
     * Checks that the right procedure description format is requested.
     *
     * @param request the request
     *
     * @throws InvalidParameterValueException if a invalid format was requested
     */
    private void checkFormat(DescribeSensorRequest request) throws InvalidParameterValueException {
        String format = request.getProcedureDescriptionFormat();
        if (format != null && !format.equals(SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL)) {
            throw new InvalidParameterValueException(DescribeSensorParams.procedureDescriptionFormat, format);
        }
    }

    /**
     * Check that no valid time was requested.
     *
     * @param request the request
     *
     * @throws InvalidParameterValueException if a valid time was requested
     */
    private void checkValidTime(DescribeSensorRequest request) throws InvalidParameterValueException {
        // we don't support valid times of sensors
        if (request.getValidTime() != null) {
            throw new InvalidParameterValueException(DescribeSensorParams.validTime,
                                                     request.getValidTime().toString());
        }
    }

    /**
     * Retrieve the SensorML description from the Sensor API.
     *
     * @param identifier the identifier
     *
     * @return the XML description
     *
     * @throws OwsExceptionReport if the retrieval fails
     */
    private String retrieveSensorML(String identifier) throws OwsExceptionReport {
        return this.sensorApiClient.getSensorML(identifier)
                .orElseThrow(() -> new NoApplicableCodeException()
                .withMessage("Could not retrieve sensor description"));
    }

    /**
     * Retrieve the SensorML description from the Sensor API and wrap it into an
     * {@code SosProcedureDescriptionUnknownType}.
     *
     * @param request the request
     *
     * @return the description
     *
     * @throws OwsExceptionReport if the retrieval fails
     */
    private SosProcedureDescriptionUnknownType retrieveDescription(DescribeSensorRequest request) throws
            OwsExceptionReport {
        String sensorDescription = retrieveSensorML(request.getProcedure());
        SosProcedureDescriptionUnknownType description = new SosProcedureDescriptionUnknownType(request.getProcedure());
        description.setDescriptionFormat(SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL);
        description.setXml(sensorDescription);
        return description;
    }

    @Override
    public boolean isSupported() {
        return true;
    }


}
