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
package org.n52.sensorweb.awi;

import static java.util.stream.Collectors.toSet;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import org.n52.sensorweb.awi.data.NearRealTimeDao;
import org.n52.sensorweb.awi.data.NearRealTimeDaoImpl;
import org.n52.sensorweb.awi.sensor.SensorApiClient;
import org.n52.sensorweb.awi.sensor.SensorApiClientImpl;
import org.n52.sensorweb.awi.sensor.json.JsonDevice;
import org.n52.sensorweb.awi.sensor.json.JsonSensorOutput;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NRTDaoImpl implements NRTDao {

    private final NearRealTimeDao nearRealTimeDao;
    private final SensorApiClient sensorApiClient;

    @Inject
    public NRTDaoImpl(NearRealTimeDao nearRealTimeDao, SensorApiClient sensorApiClient) {
        this.nearRealTimeDao = nearRealTimeDao;
        this.sensorApiClient = sensorApiClient;
    }

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

    private Stream<NRTProcedure> getProcedures(Stream<JsonDevice> stream, NRTProcedure parent) {
        return stream.map(child -> createProcedure(child, parent))
                .filter(p -> p != null && !(p.getChildren().isEmpty() && p.getOutputs().isEmpty()));
    }

    private Optional<NRTProcedure> hasData(NRTProcedure p, Set<String> dataProcedures) {
        // FIXME remove this
        String id = p.getId();
        if (dataProcedures.contains(id)) {
            return Optional.of(p);
        }

        Set<NRTProcedure> filteredChildren = p.getChildren().stream()
                .map(child -> hasData(child, dataProcedures))
                .filter(Optional::isPresent)
                .map(Optional::get).collect(toSet());

        if (filteredChildren.isEmpty()) {
            return Optional.empty();
        }

        NRTProcedure filteredProcedure = new NRTProcedure(p.getId(),
                                                          p.getShortName().orElse(null),
                                                          p.getLongName().orElse(null),
                                                          p.getDescription().orElse(null),
                                                          p.getParent().orElse(null),
                                                          p.getOutputs());
        filteredProcedure.setChildren(filteredChildren);
        return Optional.of(filteredProcedure);
    }

    @Override
    public Set<NRTProcedure> getProcedures() {
        Set<String> dataProcedures = this.nearRealTimeDao.getProcedures()
                .stream().map(NRTProcedure::getId).collect(toSet());
        List<JsonDevice> platforms = this.sensorApiClient.getPlatforms();
        return getProcedures(platforms.stream(), null)
                .map(p -> hasData(p, dataProcedures))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toSet());
    }

    private Set<NRTProcedureOutput> getOutputs(JsonDevice device) {
        return this.sensorApiClient.getSensorOutputs(device).stream()
                .map(this::toProcedureOutput).collect(toSet());
    }

    @Override
    public Map<String, NRTEnvelope> getEnvelopes() {
//        Map<String, MinMax<DateTime>> minMax = this.nearRealTimeDao.getMinMax();
//        return minMax.entrySet().stream()
//                .collect(toMap(Entry::getKey, e -> new NRTEnvelope(e.getValue(), e.getValue(), null)));
        return this.nearRealTimeDao.getEnvelopes();
    }

    private static SessionFactory createSessionFactory() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder().configure().build();
        try {
            return new Configuration().configure().buildSessionFactory(registry);
        } catch (HibernateException e) {
            StandardServiceRegistryBuilder.destroy(registry);
            throw e;
        }
    }

    public static void main(String[] args) {
        System.setProperty("org.jboss.logging.provider", "slf4j");

        URI uri = URI.create("https://handlesrv1.awi.de:8543/sensorManagement-web/rest/sensors");

        SessionFactory sessionFactory = createSessionFactory();
        try {
            NearRealTimeDao nearRealTimeDao = new NearRealTimeDaoImpl(sessionFactory);

            SensorApiClient sensorApiClient = new SensorApiClientImpl(uri);
            try {
                NRTDaoImpl nrtDao = new NRTDaoImpl(nearRealTimeDao, sensorApiClient);
                nrtDao.getProcedures().forEach(System.out::println);
            } finally {
                sensorApiClient.close();
            }

        } finally {
            sessionFactory.close();
        }
    }

    private NRTProcedureOutput toProcedureOutput(JsonSensorOutput o) {
        NRTUnit unit = new NRTUnit(o.getUnitOfMeasurement().getLongName(),
                                   o.getUnitOfMeasurement().getCode());

        return new NRTProcedureOutput(o.getName(), unit);
    }

}
