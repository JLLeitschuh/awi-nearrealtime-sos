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
import org.n52.shetland.ogc.gml.time.Time;
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
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIDescribeSensorHandler extends AbstractDescribeSensorHandler {

    private final SensorAPIClient sensorApiClient;

    @Inject
    public AWIDescribeSensorHandler(SensorAPIClient sensorApiClient) {
        super(SosConstants.SOS);
        this.sensorApiClient = sensorApiClient;
    }

    @Override
    public DescribeSensorResponse getSensorDescription(
            DescribeSensorRequest request) throws OwsExceptionReport {
        String service = request.getService();
        String version = request.getVersion();
        String procedure = request.getProcedure();
        Time validTime = request.getValidTime();
        String format = request.getProcedureDescriptionFormat();

        if (format != null && !format.equals(SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL)) {
            throw new InvalidParameterValueException(DescribeSensorParams.procedureDescriptionFormat, format);
        }

        if (validTime != null) {
            throw new InvalidParameterValueException(DescribeSensorParams.validTime, validTime.toString());
        }

        String sensorDescription = this.sensorApiClient.getSensorML(procedure)
                .orElseThrow(() -> new NoApplicableCodeException()
                .withMessage("Could not retrieve sensor description"));

        DescribeSensorResponse response = new DescribeSensorResponse();
        response.setService(service);
        response.setVersion(version);
        response.setOutputFormat(SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL);
        response.addSensorDescription(new SosProcedureDescriptionUnknownType(
                procedure, SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL, sensorDescription));
        return response;
    }
}
