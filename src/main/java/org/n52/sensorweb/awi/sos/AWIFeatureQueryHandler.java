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

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.shetland.ogc.gml.AbstractFeature;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.util.ReferencedEnvelope;
import org.n52.sos.ds.FeatureQueryHandler;
import org.n52.sos.ds.FeatureQueryHandlerQueryObject;

/**
 * TODO JavaDoc
 * @author Christian Autermann
 */
public class AWIFeatureQueryHandler implements FeatureQueryHandler{

    private static final Logger log = LoggerFactory.getLogger(AWIFeatureQueryHandler.class);
    private static final int EPSG_4326 = 4326;

    @Override
    public AbstractFeature getFeatureByID(FeatureQueryHandlerQueryObject queryObject) throws OwsExceptionReport {
        /* TODO implement org.n52.sensorweb.awi.AWIFeatureQueryHandler.getFeatureByID() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.AWIFeatureQueryHandler.getFeatureByID() not yet implemented");
    }

    @Override
    public Collection<String> getFeatureIDs(FeatureQueryHandlerQueryObject queryObject) throws OwsExceptionReport {
        /* TODO implement org.n52.sensorweb.awi.AWIFeatureQueryHandler.getFeatureIDs() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.AWIFeatureQueryHandler.getFeatureIDs() not yet implemented");
    }

    @Override
    public Map<String, AbstractFeature> getFeatures(FeatureQueryHandlerQueryObject queryObject) throws
                                                                                                       OwsExceptionReport {
        /* TODO implement org.n52.sensorweb.awi.AWIFeatureQueryHandler.getFeatures() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.AWIFeatureQueryHandler.getFeatures() not yet implemented");
    }

    @Override
    public ReferencedEnvelope getEnvelopeForFeatureIDs(FeatureQueryHandlerQueryObject queryObject) throws OwsExceptionReport {
        /* TODO implement org.n52.sensorweb.awi.AWIFeatureQueryHandler.getEnvelopeForFeatureIDs() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.AWIFeatureQueryHandler.getEnvelopeForFeatureIDs() not yet implemented");
    }

    @Override
    public String insertFeature(SamplingFeature samplingFeature, Object connection) throws OwsExceptionReport {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStorageEPSG() {
        return EPSG_4326;
    }

    @Override
    public int getStorage3DEPSG() {
        return EPSG_4326;
    }

    @Override
    public int getDefaultResponseEPSG() {
        return EPSG_4326;
    }

    @Override
    public int getDefaultResponse3DEPSG() {
        return EPSG_4326;
    }

    @Override
    @Deprecated
    public String getDatasourceDaoIdentifier() {
        return "";
    }

}
