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

import org.n52.sensorweb.awi.NRTDao;
import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.ows.exception.InvalidParameterValueException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sensorML.SensorML20Constants;
import org.n52.shetland.ogc.sos.Sos2Constants.DescribeSensorParams;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosProcedureDescription;
import org.n52.shetland.ogc.sos.SosProcedureDescriptionUnknownType;
import org.n52.shetland.ogc.sos.request.DescribeSensorRequest;
import org.n52.shetland.ogc.sos.response.DescribeSensorResponse;
import org.n52.sos.ds.AbstractDescribeSensorHandler;
import org.n52.svalbard.decode.DecoderRepository;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIDescribeSensorHandler extends AbstractDescribeSensorHandler {

    private NRTDao dao;
    private DecoderRepository decoderRepository;

    public AWIDescribeSensorHandler() {
        super(SosConstants.SOS);
    }

    @Inject
    public void setDao(NRTDao dao) {
        this.dao = dao;
    }

    @Inject
    public void setDecoderRepository(DecoderRepository decoderRepository) {
        this.decoderRepository = decoderRepository;
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

        SosProcedureDescription<?> description = getSensorDescription(procedure);
        DescribeSensorResponse response = new DescribeSensorResponse();
        response.setService(service);
        response.setVersion(version);
        response.setOutputFormat(description.getDescriptionFormat());
        response.addSensorDescription(description);
        return response;
    }

    private SosProcedureDescription<?> getSensorDescription(String identifier)
            throws OwsExceptionReport {
        return decodeDescription(identifier, retrieveDescription(identifier));
    }

    private SosProcedureDescription<?> decodeDescription(String identifier, String description)
            throws OwsExceptionReport {
//        try {
//            XmlObject xml = XmlObject.Factory.parse(description);
//            String ns = XmlHelper.getNamespace(xml);
//            XmlNamespaceDecoderKey key = new XmlNamespaceDecoderKey(ns, xml
//                                                                    .getClass());
//            Decoder<AbstractSensorML, XmlObject> decoder
//                    = this.decoderRepository.getDecoder(key);
//            if (decoder == null) {
//                throw new NoDecoderForKeyException(key);
//            }
//            return decoder.decode(xml);
        return new SosProcedureDescriptionUnknownType(identifier, description,
                SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL);
//        } catch (XmlException | DecodingException ex) {
//            throw new NoApplicableCodeException().causedBy(ex);
//        }
    }

    private String retrieveDescription(String identifier) throws
            OwsExceptionReport {
        return this.dao.getDescription(identifier)
                .orElseThrow(() -> new NoApplicableCodeException()
                .withMessage("Could not retrieve sensor description"));
    }

}
