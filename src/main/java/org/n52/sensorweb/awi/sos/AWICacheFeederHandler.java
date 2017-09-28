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

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.spatial.criterion.SpatialProjections;
import org.joda.time.DateTime;

import org.n52.janmayen.Optionals;
import org.n52.janmayen.function.Consumers;
import org.n52.janmayen.function.Functions;
import org.n52.janmayen.function.Predicates;
import org.n52.sensorweb.awi.data.FeatureCache;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Expedition;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sensorweb.awi.sensor.SensorAPIClient;
import org.n52.sensorweb.awi.sensor.json.JsonDevice;
import org.n52.sensorweb.awi.sensor.json.JsonSensorOutput;
import org.n52.sensorweb.awi.util.SpaceTimeEnvelope;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.features.SfConstants;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sensorML.SensorML20Constants;
import org.n52.shetland.util.MinMax;
import org.n52.shetland.util.ReferencedEnvelope;
import org.n52.sos.cache.SosContentCache.ComponentAggregation;
import org.n52.sos.cache.SosContentCache.TypeInstance;
import org.n52.sos.cache.SosWritableContentCache;
import org.n52.sos.ds.CacheFeederHandler;
import org.n52.sos.ds.hibernate.util.AbstractSessionDao;
import org.n52.sos.ds.hibernate.util.DefaultResultTransfomer;
import org.n52.sos.ds.hibernate.util.PropertyPath;

import com.google.common.base.Strings;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * {@link CacheFeederHandler} for the AWI Nearrealtime database.
 *
 * @author Christian Autermann
 */
public class AWICacheFeederHandler extends AbstractSessionDao implements CacheFeederHandler {

    private static final int EPSG_4326 = 4326;
    private static final String OBSERVATION_TYPE = OmConstants.OBS_TYPE_MEASUREMENT;
    private static final String FEATURE_TYPE = SfConstants.SAMPLING_FEAT_TYPE_SF_SPATIAL_SAMPLING_FEATURE;
    private static final String PROCEDURE_DESCRIPTION_TYPE = SensorML20Constants.SENSORML_20_OUTPUT_FORMAT_URL;
    private static final String FEATURE_ROLE = SfConstants.SAMPLING_FEAT_TYPE_SF_SPATIAL_SAMPLING_FEATURE;

    private final FeatureCache featureCache;
    private final SensorAPIClient sensorApiClient;

    /**
     * Create a new {@code AWICacheFeederHandler}.
     *
     * @param sessionFactory  the session factory
     * @param featureCache    the feature cache
     * @param sensorAPIClient the sensor API client
     */
    @Inject
    public AWICacheFeederHandler(SessionFactory sessionFactory,
                                 FeatureCache featureCache,
                                 SensorAPIClient sensorAPIClient) {
        super(sessionFactory);
        this.featureCache = featureCache;
        this.sensorApiClient = sensorAPIClient;
    }

    @Override
    public void updateCacheOfferings(SosWritableContentCache cache, Collection<String> offerings) throws
            OwsExceptionReport {
        updateCache(cache);
    }

    @Override
    public void updateCache(SosWritableContentCache cache) throws OwsExceptionReport {

        Map<String, SpaceTimeEnvelope> envelopes = getEnvelopes();
        // we only support EPSG:4326
        cache.setDefaultEPSGCode(EPSG_4326);
        cache.addEpsgCode(EPSG_4326);

        // only support SensorML 2.0
        cache.setRequestableProcedureDescriptionFormat(Collections.singleton(PROCEDURE_DESCRIPTION_TYPE));

        getProcedures().forEach(procedure -> addProcedure(cache, procedure, envelopes));

        // recalculate the global bounding boxes
        cache.recalculateGlobalEnvelope();
        cache.recalculatePhenomenonTime();
        cache.recalculateResultTime();

        // done
        cache.setLastUpdateTime(DateTime.now());
    }

    /**
     * Add the procedure to the cache.
     *
     * @param cache     the cache
     * @param procedure the procedure
     * @param envelopes the collection of all envelopes
     */
    private void addProcedure(SosWritableContentCache cache,
                              NRTProcedure procedure,
                              Map<String, SpaceTimeEnvelope> envelopes) {
        String procedureId = procedure.getId();
        String offeringId = procedureId;
        Set<String> featureIds = getFeaturesOfInterest(procedure);

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
        cache.addFeaturesOfInterest(featureIds);

        // feature <-> offering
        featureIds.stream()
                .forEach(Consumers.curryFirst(cache::addFeatureOfInterestForOffering, offeringId)
                        .andThen(Consumers.curryFirst(cache::addRelatedFeatureForOffering, offeringId))
                        .andThen(Consumers.currySecond(cache::addProcedureForFeatureOfInterest, procedureId))
                        .andThen(Consumers.currySecond(cache::setRolesForRelatedFeature, Collections
                                                       .singleton(FEATURE_ROLE))));

        procedure.getOutputs().forEach(output -> {
            String observableProperty = output.getCode();
            // output <-> procedure
            cache.addObservablePropertyForProcedure(procedureId, observableProperty);
            cache.addProcedureForObservableProperty(observableProperty, procedureId);
            // output <-> offering
            cache.addOfferingForObservableProperty(observableProperty, offeringId);
            cache.addObservablePropertyForOffering(offeringId, observableProperty);
        });

        getEnvelope(envelopes, procedureId).ifPresent(e -> {
            // phenomenon time <-> offering
            cache.setMinPhenomenonTimeForOffering(offeringId, e.getTime().getMinimum());
            cache.setMaxPhenomenonTimeForOffering(offeringId, e.getTime().getMaximum());
            // phenomenon time <-> procedure
            cache.setMinPhenomenonTimeForProcedure(procedureId, e.getTime().getMinimum());
            cache.setMaxPhenomenonTimeForProcedure(procedureId, e.getTime().getMaximum());
            // result time <-> offering
            cache.setMinResultTimeForOffering(offeringId, e.getTime().getMinimum());
            cache.setMaxResultTimeForOffering(offeringId, e.getTime().getMaximum());
            // envelope <-> offering
            cache.setEnvelopeForOffering(offeringId, new ReferencedEnvelope(e.getSpace(), EPSG_4326));
        });

        // add the child procedures
        procedure.getChildren().forEach(child -> addProcedure(cache, child, envelopes));
    }

    /**
     * Get the envelope for the specified procedure. This will check if the procedure has an envelope, if it's children
     * have envelopes and if it's parents have envelopes.
     *
     * @param envelopes   the envelopes
     * @param procedureId the procedure identifier
     *
     * @return the envelope
     */
    private Optional<SpaceTimeEnvelope> getEnvelope(Map<String, SpaceTimeEnvelope> envelopes, String procedureId) {
        return Optionals.or(
                () -> Optional.ofNullable(envelopes.get(procedureId)),
                () -> getChildEnvelope(envelopes, procedureId),
                () -> getParentEnvelope(envelopes, procedureId));
    }

    /**
     * Get the envelope for all children of the specified procedure.
     *
     * @param envelopes all envelopes
     * @param procedure the procedure
     *
     * @return the envelope
     */
    private Optional<SpaceTimeEnvelope> getChildEnvelope(Map<String, SpaceTimeEnvelope> envelopes, String procedure) {
        return Optional.of(envelopes.entrySet().stream()
                .filter(e -> e.getKey().startsWith(procedure))
                .map(Entry::getValue)
                .collect(Functions.curry(SpaceTimeEnvelope::new, procedure),
                         SpaceTimeEnvelope::extend,
                         SpaceTimeEnvelope::extend))
                .filter(Predicates.not(SpaceTimeEnvelope::isEmpty));
    }

    /**
     * Get the envelope of the parent of this procedure.
     *
     * @param envelopes all envelopes
     * @param procedure the procedure
     *
     * @return the envelope
     */
    private Optional<SpaceTimeEnvelope> getParentEnvelope(Map<String, SpaceTimeEnvelope> envelopes, String procedure) {
        return getParentProcedure(procedure).flatMap(parent -> getEnvelope(envelopes, parent));
    }

    /**
     * Get the parent procedure from the procedure identifier.
     *
     * @param procedure the identifier
     *
     * @return the parent procedure
     */
    private Optional<String> getParentProcedure(String procedure) {
        int idx = procedure.lastIndexOf(':');
        if (idx > 0 && idx < procedure.length() - 1) {
            return Optional.of(procedure.substring(0, idx));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the features of interest for the specified procedure.
     *
     * @param procedure the procedure
     *
     * @return the features of interest
     */
    private Set<String> getFeaturesOfInterest(NRTProcedure procedure) {
        String platform = procedure.getPlatform().getId();
        Set<String> featureIds = this.featureCache.getFeatureIds(platform);
        if (featureIds.isEmpty()) {
            return Collections.singleton(platform);
        } else {
            return featureIds;
        }
    }

    /**
     * Get the spatio-temporal envelope for all procedures with data in the database.
     *
     * @return the envelopes
     */
    public Map<String, SpaceTimeEnvelope> getEnvelopes() {
        QueryContext ctx = QueryContext.forData();

        DefaultResultTransfomer<SpaceTimeEnvelope> transformer = t -> {
            String id = Arrays.stream(t, 0, 2).map(String::valueOf).collect(joining(":"));
            MinMax<DateTime> time = new MinMax<>(new DateTime((Date) t[2]), new DateTime((Date) t[3]));
            Envelope geom = ((Geometry) t[4]).getEnvelopeInternal();
            return new SpaceTimeEnvelope(id, time, geom);
        };

        return query(s -> {
            Criteria mobile = s.createCriteria(Data.class)
                    .setComment("Getting envelopes for mobile data")
                    .createAlias(ctx.getDataPath(Data.SENSOR), ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .createAlias(ctx.getPlatformPath(Platform.EXPEDITIONS), "e")
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.min(ctx.getDataPath(Data.TIME)))
                            .add(Projections.max(ctx.getDataPath(Data.TIME)))
                            .add(SpatialProjections.extent(ctx.getDataPath(Data.GEOMETRY))))
                    .add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                    .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                    .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                    .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true))
                    .add(Restrictions.isNotNull(ctx.getDataPath(Data.GEOMETRY)))
                    .add(Restrictions.geProperty(ctx.getDataPath(Data.TIME), PropertyPath.of("e", Expedition.BEGIN)))
                    .add(Restrictions.leProperty(ctx.getDataPath(Data.TIME), PropertyPath.of("e", Expedition.END)));
            Criteria stationary = s.createCriteria(Data.class)
                    .setComment("Getting envelopes for stationary data")
                    .createAlias(ctx.getDataPath(Data.SENSOR), ctx.getSensor())
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .setProjection(Projections.projectionList()
                            .add(Projections.groupProperty(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.groupProperty(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.min(ctx.getDataPath(Data.TIME)))
                            .add(Projections.max(ctx.getDataPath(Data.TIME)))
                            .add(SpatialProjections.extent(ctx.getPlatformPath(Platform.GEOMETRY))))
                    .add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                    .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                    .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                    .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true))
                    .add(Restrictions.isEmpty(ctx.getPlatformPath(Platform.EXPEDITIONS)))
                    .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.GEOMETRY)));

            return Stream.of(mobile, stationary)
                    .map(Functions.currySecond(Criteria::setReadOnly, true))
                    .map(Functions.currySecond(Criteria::setResultTransformer, transformer))
                    .map(Criteria::list)
                    .flatMap(List<SpaceTimeEnvelope>::stream)
                    .collect(toMap(SpaceTimeEnvelope::getIdentifier, Function.identity()));
        });
    }

    /**
     * Create a child procedure out of the device.
     *
     * @param device the device
     * @param parent the parent procedure
     *
     * @return the procedure
     */
    private NRTProcedure createProcedure(JsonDevice device, NRTProcedure parent) {
        if (device.getUrn() == null) {
            return null;
        }
        NRTProcedure procedure = new NRTProcedure(device.getUrn(),
                                                  device.getShortName(),
                                                  device.getLongName(),
                                                  device.getDescription(),
                                                  parent,
                                                  getOutputs(device));
        List<JsonDevice> children = this.sensorApiClient.getChildren(device);
        procedure.setChildren(getProcedures(children.stream(), procedure).collect(toSet()));
        return procedure;
    }

    /**
     * Create child procedures from the stream of devices.
     *
     * @param stream the stream of child devices
     * @param parent the parent procedure
     *
     * @return the stream of procedures
     */
    private Stream<NRTProcedure> getProcedures(Stream<JsonDevice> stream, NRTProcedure parent) {
        return stream.map(child -> createProcedure(child, parent))
                .filter(p -> p != null && !(p.getChildren().isEmpty() && p.getOutputs().isEmpty()));
    }

    /**
     * Checks if the procedure {@code p} has data in the database.
     *
     * @param p              the procedure
     * @param dataProcedures the procedure identifiers with data
     *
     * @return the procedure
     */
    private boolean hasData(NRTProcedure p, Map<String, Set<String>> dataProcedures) {
        String id = p.getId();

        if (!dataProcedures.containsKey(id)) {
            p.clearOutputs();
        } else {
            Set<String> outputs = dataProcedures.get(id);
            p.filterOutputs(output -> outputs.contains(output.getCode()));
        }

        p.filterChildren(child -> hasData(child, dataProcedures));

        return !p.getChildren().isEmpty() || !p.getOutputs().isEmpty();
    }

    /**
     * Get the procedures that have a SensorML description in the sensor API as well as data in the database. Including
     * any parent procedures whose children have data.
     *
     * @return the procedures
     */
    private Set<NRTProcedure> getProcedures() {
        Map<String, Set<String>> dataProcedures = getDbProcedures();
        List<JsonDevice> platforms = this.sensorApiClient.getPlatforms();
        return getProcedures(platforms.stream(), null)
                .filter(p -> hasData(p, dataProcedures))
                .collect(toSet());
    }

    /**
     * Get the procedures from the database.
     *
     * @return the procedures and their outputs
     */
    @SuppressWarnings("unchecked")
    private Map<String, Set<String>> getDbProcedures() {
        QueryContext ctx = QueryContext.forSensor();
        return query((Session session) -> {
            Criteria c = session.createCriteria(Sensor.class)
                    .setComment("Getting procedures")
                    .createAlias(ctx.getSensorPath(Sensor.DEVICE), ctx.getDevice())
                    .createAlias(ctx.getDevicePath(Device.PLATFORM), ctx.getPlatform())
                    .setProjection(Projections.projectionList()
                            .add(Projections.property(ctx.getPlatformPath(Platform.CODE)))
                            .add(Projections.property(ctx.getDevicePath(Device.CODE)))
                            .add(Projections.property(ctx.getSensorPath(Sensor.CODE))))
                    .add(Restrictions.isNotNull(ctx.getSensorPath(Sensor.CODE)))
                    .add(Restrictions.isNotNull(ctx.getDevicePath(Device.CODE)))
                    .add(Restrictions.isNotNull(ctx.getPlatformPath(Platform.CODE)))
                    .add(Restrictions.eq(ctx.getPlatformPath(Platform.PUBLISHED), true));
            return ((List<Object[]>) c.list())
                    .stream().collect(groupingBy(
                            t -> Arrays.stream(t, 0, 2).map(String::valueOf).map(String::toLowerCase)
                                    .collect(joining(":")),
                            mapping(t -> (String) t[2], toSet())));
        });
    }

    private Set<NRTProcedureOutput> getOutputs(JsonDevice device) {
        return this.sensorApiClient.getSensorOutputs(device).stream()
                .map(this::toProcedureOutput).collect(toSet());
    }

    /**
     * Create a {@code NRTProcedureOutput} for the {@code JsonSensorOutput}.
     *
     * @param o the sensor output
     *
     * @return the procedure output
     */
    private NRTProcedureOutput toProcedureOutput(JsonSensorOutput o) {
        NRTUnit unit = new NRTUnit(o.getUnitOfMeasurement().getLongName(),
                                   o.getUnitOfMeasurement().getCode());
        return new NRTProcedureOutput(o.getCode(), o.getName(), unit);
    }

    /**
     * A procedure that can have a parent procedure and multiple child procedures and outputs.
     */
    private static class NRTProcedure {

        private final String id;
        private final Optional<String> longName;
        private final Optional<String> shortName;
        private final Optional<String> description;
        private final Optional<NRTProcedure> parent;
        private Set<NRTProcedure> children = Collections.emptySet();
        private Set<NRTProcedureOutput> outputs;

        /**
         * Create a new {@code NRTProcedure}.
         *
         * @param id          the id
         * @param shortName   the short name
         * @param longName    the long name
         * @param description the description
         * @param parent      the parent procedure
         * @param outputs     the outputs of this procedure
         */
        NRTProcedure(String id, String shortName, String longName, String description, NRTProcedure parent,
                     Set<NRTProcedureOutput> outputs) {
            this.id = Objects.requireNonNull(Strings.emptyToNull(id));
            this.longName = Optional.ofNullable(Strings.emptyToNull(longName));
            this.shortName = Optional.ofNullable(Strings.emptyToNull(shortName));
            this.description = Optional.ofNullable(Strings.emptyToNull(description));
            this.parent = Optional.ofNullable(parent);
            this.outputs = Optional.ofNullable(outputs).orElseGet(Collections::emptySet);
        }

        /**
         * Get the parent of this procedure.
         *
         * @return the parent
         */
        Optional<NRTProcedure> getParent() {
            return this.parent;
        }

        /**
         * Get the platform of this procedure.
         *
         * @return the platform
         */
        NRTProcedure getPlatform() {
            NRTProcedure elem = this;
            while (elem.getParent().isPresent()) {
                elem = elem.getParent().get();
            }
            return elem;
        }

        /**
         * Get the children of this procedure.
         *
         * @return the children
         */
        Set<NRTProcedure> getChildren() {
            return Collections.unmodifiableSet(this.children);
        }

        /**
         * Set the children of this procedure-
         *
         * @param children the children
         */
        void setChildren(Set<NRTProcedure> children) {
            this.children = Optional.ofNullable(children).orElseGet(Collections::emptySet);
        }

        /**
         * Get the outputs of this procedure.
         *
         * @return the outputs
         */
        Set<NRTProcedureOutput> getOutputs() {
            return Collections.unmodifiableSet(this.outputs);
        }

        void clearOutputs() {
            this.outputs = Collections.emptySet();
        }

        void filterOutputs(Predicate<NRTProcedureOutput> predicate) {
            if (!this.outputs.isEmpty()) {
                this.outputs.removeIf(predicate.negate());
            }
        }

        void filterChildren(Predicate<NRTProcedure> predicate) {
            if (!this.children.isEmpty()) {
                this.children.removeIf(predicate.negate());
            }
        }

        /**
         * Get the id of this procedure.
         *
         * @return the id
         */
        String getId() {
            return id;
        }

        /**
         * Get the long name of this procedure.
         *
         * @return the long name
         */
        Optional<String> getLongName() {
            return longName;
        }

        /**
         * Get the short name of this procedure.
         *
         * @return the short name
         */
        Optional<String> getShortName() {
            return shortName;
        }

        /**
         * Get the description of this procedure.
         *
         * @return the description
         */
        Optional<String> getDescription() {
            return description;
        }
    }

    /**
     * The output of an procudure.
     */
    private static class NRTProcedureOutput {
        private final String code;
        private final String name;
        private final NRTUnit unit;

        /**
         * Create a new {@code NRTProcedureOutput}.
         *
         * @param code the code
         * @param name the name
         * @param unit the unit
         */
        NRTProcedureOutput(String code, String name, NRTUnit unit) {
            this.code = code;
            this.name = name;
            this.unit = unit;
        }

        /**
         * Get the name.
         *
         * @return the name
         */
        String getName() {
            return name;
        }

        /**
         * Get the unit.
         *
         * @return the unit
         */
        NRTUnit getUnit() {
            return unit;
        }

        /**
         * Get the code.
         *
         * @return the code
         */
        String getCode() {
            return code;
        }
    }

    /**
     * The unit of an output.
     */
    private static class NRTUnit {

        private final String name;
        private final String unit;

        /**
         * Create a new unit.
         *
         * @param name the name
         * @param unit the unit
         */
        NRTUnit(String name, String unit) {
            this.name = name;
            this.unit = unit;
        }

        /**
         * Get the name.
         *
         * @return the name
         */
        String getName() {
            return name;
        }

        /**
         * Get the unit.
         *
         * @return the unit
         */
        String getUnit() {
            return unit;
        }
    }
}
