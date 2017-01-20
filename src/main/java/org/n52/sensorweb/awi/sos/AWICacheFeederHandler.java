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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.joda.time.DateTime;

import org.n52.sensorweb.awi.NRTDao;
import org.n52.sensorweb.awi.NRTEnvelope;
import org.n52.sensorweb.awi.NRTProcedure;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.features.SfConstants;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sensorML.SensorML20Constants;
import org.n52.shetland.util.ReferencedEnvelope;
import org.n52.sos.cache.SosContentCache.ComponentAggregation;
import org.n52.sos.cache.SosContentCache.TypeInstance;
import org.n52.sos.cache.SosWritableContentCache;
import org.n52.sos.ds.CacheFeederHandler;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class AWICacheFeederHandler implements CacheFeederHandler {

    private static final int EPSG_4326 = 4326;

    private static final String OBSERVATION_TYPE = OmConstants.OBS_TYPE_MEASUREMENT;
    private static final String FEATURE_TYPE = SfConstants.SAMPLING_FEAT_TYPE_SF_SPATIAL_SAMPLING_FEATURE;
    private static final String PROCEDURE_DESCRIPTION_TYPE = SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL;
    private final NRTDao dao;

    @Inject
    public AWICacheFeederHandler(NRTDao dao) {
        this.dao = dao;
    }

    @Override
    public void updateCacheOfferings(SosWritableContentCache cache, Collection<String> offerings) throws OwsExceptionReport {
        updateCache(cache);
    }

    public Map<String, NRTEnvelope> getEnvelopes() {
        Map<String, NRTEnvelope> envelopes = this.dao.getEnvelopes();
        return envelopes;
    }

    @Override
    public void updateCache(SosWritableContentCache cache) throws OwsExceptionReport {

        Map<String, NRTEnvelope> envelopes = getEnvelopes();
        // we only support EPSG:4326
        cache.setDefaultEPSGCode(EPSG_4326);
        cache.addEpsgCode(EPSG_4326);

        // only support SensorML 2.0
        cache.setRequestableProcedureDescriptionFormat(Collections.singleton(PROCEDURE_DESCRIPTION_TYPE));

        this.dao.getProcedures().forEach(procedure -> addProcedure(cache, procedure, envelopes));

        // recalculate the global bounding boxes
        cache.recalculateGlobalEnvelope();
        cache.recalculatePhenomenonTime();
        cache.recalculateResultTime();

        // done
        cache.setLastUpdateTime(DateTime.now());
    }

    private String getFeatureOfInterest(NRTProcedure procedure) {
        if (procedure.getParent().isPresent()) {
            return getFeatureOfInterest(procedure.getParent().get());
        } else {
            return procedure.getId();
        }
    }

    private void addProcedure(SosWritableContentCache cache, NRTProcedure procedure, Map<String, NRTEnvelope> envelopes) {
        String procedureId = procedure.getId();
        String offeringId = procedureId;
        String featureId = getFeatureOfInterest(procedure);

        cache.addProcedure(procedureId);
        cache.addTypeInstanceProcedure(TypeInstance.INSTANCE, procedureId);

        // offering
        cache.addOffering(offeringId);

        // we only have measurements
        cache.addObservationTypesForOffering(offeringId, OBSERVATION_TYPE);

        // procedure <-> offering
        cache.addOfferingForProcedure(procedureId, offeringId);
        cache.addProcedureForOffering(offeringId, procedureId);

        // procedure <-> parent procedure
        procedure.getParent().ifPresent(parent -> cache.addParentProcedure(procedureId, parent.getId()));

        if (!procedure.getChildren().isEmpty()) {
            cache.addComponentAggregationProcedure(ComponentAggregation.AGGREGATION, procedureId);
        }
        if (!procedure.getOutputs().isEmpty()) {
            cache.addComponentAggregationProcedure(ComponentAggregation.COMPONENT, procedureId);
        }

        cache.addFeatureOfInterestTypesForOffering(offeringId, FEATURE_TYPE);

        // feature of interest
        cache.addFeatureOfInterest(featureId);

        // feature <-> offering
        cache.addFeatureOfInterestForOffering(offeringId, featureId);
        cache.addProcedureForFeatureOfInterest(featureId, procedureId);


        procedure.getOutputs().forEach(output -> {
            String observableProperty = output.getName();
            // output <-> procedure
            cache.addObservablePropertyForProcedure(procedureId, observableProperty);
            cache.addProcedureForObservableProperty(observableProperty, procedureId);
            // output <-> offering
            cache.addOfferingForObservableProperty(observableProperty, offeringId);
            cache.addObservablePropertyForOffering(offeringId, observableProperty);
        });

        getEnvelope(envelopes, procedureId).ifPresent(e -> {
            // phenomenon time <-> offering
            cache.setMinPhenomenonTimeForOffering(offeringId, e.getPhenomenonTime().getMinimum());
            cache.setMaxPhenomenonTimeForOffering(offeringId, e.getPhenomenonTime().getMaximum());
            // phenomenon time <-> procedure
            cache.setMinPhenomenonTimeForProcedure(procedureId, e.getPhenomenonTime().getMinimum());
            cache.setMaxPhenomenonTimeForProcedure(procedureId, e.getPhenomenonTime().getMaximum());
            // result time <-> offering
            cache.setMinResultTimeForOffering(offeringId, e.getResultTime().getMinimum());
            cache.setMaxResultTimeForOffering(offeringId, e.getResultTime().getMaximum());
            // envelope <-> offering
            cache.setEnvelopeForOffering(offeringId, new ReferencedEnvelope(e.getEnvelope(), EPSG_4326));
        });

        // add the child procedures
        procedure.getChildren().forEach(child ->  addProcedure(cache, child, envelopes));
    }

    private Optional<NRTEnvelope> getEnvelope(Map<String, NRTEnvelope> envelopes, String procedureId) {
        NRTEnvelope envelope = envelopes.get(procedureId);

        if (envelope == null) {
            int idx = procedureId.indexOf(':');
            if (idx > 0 && idx < procedureId.length() - 1) {
                envelope = envelopes.get(procedureId.substring(idx + 1));
            }
        }

        return Optional.ofNullable(envelope);
    }

}
