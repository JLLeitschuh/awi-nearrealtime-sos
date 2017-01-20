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
package org.n52.sensorweb.awi.data;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.DateTime;

import org.n52.janmayen.stream.Streams;
import org.n52.sensorweb.awi.NRTEnvelope;
import org.n52.sensorweb.awi.NRTProcedure;
import org.n52.sensorweb.awi.NRTProcedureOutput;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.DataView;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.shetland.util.MinMax;

import com.vividsolutions.jts.geom.Envelope;


/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NearRealTimeDaoImpl extends AbstractSessionDao implements NearRealTimeDao {
    private static final Property CODE = Property.forName("code");
    private static final Property ID = Property.forName("id");
    public NearRealTimeDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Device> getDevices() {
        return query(s -> s.createCriteria(Device.class).add(CODE.isNotNull()).list());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Sensor> getSensors() {
        return query(s -> s.createCriteria(Sensor.class).add(CODE.isNotNull()).list());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Platform> getPlatforms() {
        return query(s -> s.createCriteria(Platform.class).add(CODE.isNotNull()).list());
    }

    @Override
    public Optional<Device> getDevice(int id) {
        return queryOptional(s -> s.createCriteria(Device.class).add(ID.eq(id)).uniqueResult());
    }

    @Override
    public Optional<Sensor> getSensor(int id) {
        return queryOptional(s -> s.createCriteria(Sensor.class).add(ID.eq(id)).uniqueResult());
    }

    @Override
    public Optional<Platform> getPlatform(int id) {
        return queryOptional(s -> s.createCriteria(Platform.class).add(ID.eq(id)).uniqueResult());
    }

    @Override
    public Optional<Device> getDevice(String code) {
        return queryOptional(s -> s.createCriteria(Device.class).add(CODE.eq(code).ignoreCase()).uniqueResult());
    }

    @Override
    public Optional<Sensor> getSensor(String code) {
        return queryOptional(s -> s.createCriteria(Sensor.class).add(CODE.eq(code).ignoreCase()).uniqueResult());
    }

    @Override
    public Optional<Platform> getPlatform(String code) {
        return queryOptional(s -> s.createCriteria(Platform.class).add(CODE.eq(code).ignoreCase()).uniqueResult());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, MinMax<DateTime>> getMinMax() {
        return query(s -> {
            Criteria c = s.createCriteria(Data.class, "data");
            c.createAlias("data.sensor", "sensor");
            c.createAlias("sensor.device", "device");
            c.createAlias("device.platform", "platform");
            return (List<Object[]>) c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty("platform.code"))
                    .add(Projections.groupProperty("device.code"))
                    .add(Projections.min("data.time"))
                    .add(Projections.max("data.time")))
                    .add(Restrictions.isNotNull("platform.code"))
                    .add(Restrictions.isNotNull("device.code"))
                    .add(Restrictions.isNotNull("sensor.code"))
                .list();
        }).stream().collect(toMap(t -> String.format("%s:%s", t[0], t[1]).toLowerCase(),
                                  t -> new MinMax<>((DateTime) t[2], (DateTime) t[3]),
                                  Streams.throwingMerger()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, NRTEnvelope> getEnvelopes() {
        return query(s -> {
            Criteria c = s.createCriteria(DataView.class, "data");
            c.createAlias("data.sensor", "sensor");
            c.createAlias("sensor.device", "device");
            c.createAlias("device.platform", "platform");
            return (List<Object[]>) c.setProjection(Projections.projectionList()
                        .add(Projections.groupProperty("platform.type"))
                        .add(Projections.groupProperty("platform.code"))
                        .add(Projections.groupProperty("device.code"))
                        .add(Projections.min("data.time"))
                        .add(Projections.max("data.time"))
                        .add(Projections.min("data.longitude"))
                        .add(Projections.max("data.longitude"))
                        .add(Projections.min("data.latitude"))
                        .add(Projections.max("data.latitude")))
                    .add(Restrictions.isNotNull("platform.code"))
                    .add(Restrictions.isNotNull("device.code"))
                    .add(Restrictions.isNotNull("data.latitude"))
                    .add(Restrictions.isNotNull("data.longitude"))
                    .add(Restrictions.isNotNull("data.time"))
                    .list();
        }).stream().collect(toMap(
                t -> String.format("%s:%s:%s", t[0], t[1], t[2]).toLowerCase(),
                t -> {

                    MinMax<DateTime> time = new MinMax<>((DateTime) t[3], (DateTime) t[4]);
                    Envelope envelope = new Envelope((double) t[5], (double) t[6],
                                                     (double) t[7], (double) t[8]);
                    return new NRTEnvelope(time, time, envelope);

                },
                Streams.throwingMerger()));
    }

    @Override
    public Optional<Sensor> getSeries(String urn) {
        String[] split = urn.split(":");
        if (split.length != 3) {
            return Optional.empty();
        }
        String platform = split[0];
        String device = split[1];
        String sensor = split[2];
        return queryOptional(s -> getSeries(s, platform, device, sensor));
    }

    private Sensor getSeries(Session s, String platformCode, String deviceCode, String sensorCode)
            throws HibernateException {
        return (Sensor) s
                .createCriteria(Sensor.class)
                .add(CODE.eq(sensorCode).ignoreCase())
                .createCriteria("device", JoinType.INNER_JOIN)
                .add(CODE.eq(deviceCode).ignoreCase())
                .createCriteria("platform", JoinType.INNER_JOIN)
                .add(CODE.eq(platformCode).ignoreCase())
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<NRTProcedure> getProcedures() {
        return query((Session session) -> {
            Criteria c = session.createCriteria(Sensor.class, "sensor")
                    .createAlias("sensor.device", "device")
                    .createAlias("device.platform", "platform")
                    .setProjection(Projections.projectionList()
                            .add(Projections.property("platform.type"))
                            .add(Projections.property("platform.code"))
                            .add(Projections.property("device.code"))
                            .add(Projections.property("sensor.code")))
                    .add(Restrictions.isNotNull("platform.code"))
                    .add(Restrictions.isNotNull("device.code"))
                    .add(Restrictions.isNotNull("sensor.code"));
            return ((List<Object[]>) c.list()).stream()
                    .collect(groupingBy(t -> String.format("%s:%s:%s", t[0], t[1], t[2]).toLowerCase(),
                                        mapping(t -> (String) t[3], toList())))
                    .entrySet()
                    .stream()
                    .map(e -> new NRTProcedure(e.getKey(), null, null, null, null, asOutputs(e.getValue())))
                    .collect(toList());
        });
    }

    private Set<NRTProcedureOutput> asOutputs(Collection<String> outputs) {
        return outputs.stream().map(o -> new NRTProcedureOutput(o, null, null)).collect(toSet());
    }

    public static class Series {
        private final Platform platform;
        private final Sensor sensor;
        private final Device device;

        public Series(Platform platform, Device device, Sensor sensor) {
            this.platform = Objects.requireNonNull(platform);
            this.device = Objects.requireNonNull(device);
            this.sensor = Objects.requireNonNull(sensor);
        }

        public Platform getPlatform() {
            return platform;
        }

        public Sensor getSensor() {
            return sensor;
        }

        public Device getDevice() {
            return device;
        }

        @Override
        public String toString() {
            return "Series{" + "platform=" + platform + ", sensor=" + sensor + ", device=" + device + '}';
        }
    }
}
