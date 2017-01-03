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

import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.request.GetFeatureOfInterestRequest;
import org.n52.shetland.ogc.sos.response.GetFeatureOfInterestResponse;
import org.n52.sos.ds.AbstractGetFeatureOfInterestHandler;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWIGetFeatureOfInterestHandler extends AbstractGetFeatureOfInterestHandler {

    public AWIGetFeatureOfInterestHandler() {
        super(SosConstants.SOS);
    }

    @Override
    public GetFeatureOfInterestResponse getFeatureOfInterest(GetFeatureOfInterestRequest request) throws OwsExceptionReport {
        /* TODO implement org.n52.sensorweb.awi.AWIGetFeatureOfInterestHandler.getFeatureOfInterest() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.AWIGetFeatureOfInterestHandler.getFeatureOfInterest() not yet implemented");
    }

}
