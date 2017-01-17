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

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.sensorweb.awi.NRTEnvelope;
import org.n52.sensorweb.awi.NRTProcedure;
import org.n52.sensorweb.awi.NRTProcedureOutput;
import org.n52.sensorweb.awi.data.entities.Data;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;
import org.n52.sensorweb.awi.util.Streams;
import org.n52.shetland.util.MinMax;

import com.vividsolutions.jts.geom.Envelope;


/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NearRealTimeDaoImpl extends AbstractSessionDao implements NearRealTimeDao {
    private static final Logger LOG = LoggerFactory.getLogger(NearRealTimeDaoImpl.class);
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
                    .add(Projections.min("data.date"))
                    .add(Projections.max("data.date")))
                    .add(Restrictions.isNotNull("platform.code"))
                    .add(Restrictions.isNotNull("device.code"))
                    .add(Restrictions.isNotNull("sensor.code"))
                .list();
        }).stream().collect(toMap(t -> String.format("%s:%s", t[0], t[1]).toLowerCase(),
                                 t -> new MinMax<>((DateTime) t[2], (DateTime) t[3]),
                                 Streams.throwOnDuplicateKey()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, NRTEnvelope> getEnvelopes() {

        return query(s -> {
            return (List<Object[]>) s.createSQLQuery(
                    "SELECT\n" +
                    "  p.code AS platform,\n" +
                    "  d.code AS device,\n" +
                    "  min((SELECT min(data.date) FROM data WHERE data.sensor_id = pos.sensor_id)) AS min_time,\n" +
                    "  max((SELECT max(data.date) FROM data WHERE data.sensor_id = pos.sensor_id)) AS max_time,\n" +
                    "  min((SELECT min(data.value) FROM data WHERE data.sensor_id = pos.latitude)) AS min_lat,\n" +
                    "  max((SELECT max(data.value) FROM data WHERE data.sensor_id = pos.latitude)) AS max_lat,\n" +
                    "  min((SELECT min(data.value) FROM data WHERE data.sensor_id = pos.longitude)) AS min_lon,\n" +
                    "  max((SELECT max(data.value) FROM data WHERE data.sensor_id = pos.longitude)) AS max_lon\n" +
                    "FROM position AS pos\n" +
                    "  INNER JOIN sensor   AS s ON (pos.sensor_id = s.sensor_id)\n" +
                    "  INNER JOIN device   AS d ON (pos.device_id = d.device_id)\n" +
                    "  INNER JOIN platform AS p ON (pos.platform_id = p.platform_id)\n" +
                    "WHERE p.code IS NOT NULL \n" +
                    "  AND d.code IS NOT NULL \n" +
                    "  AND s.code IS NOT NULL \n" +
                    "GROUP BY p.code, d.code").list();
        }).stream().collect(Collectors.toMap(
                t -> String.format("%s:%s", t[0], t[1]).toLowerCase(),
                t -> {
                    MinMax<DateTime> time = new MinMax<>(
                            new DateTime(((Timestamp) t[2]).getTime()),
                            new DateTime(((Timestamp) t[3]).getTime()));
                    Envelope envelope = new Envelope((double) t[6],
                                                     (double) t[7],
                                                     (double) t[4],
                                                     (double) t[5]);
                    return new NRTEnvelope(time, time, envelope);
                }));


//        return query(s -> {
//            return (List<Object[]>) s.createQuery(
//                    "select\n" +
//                "  p.code AS platform,\n" +
//                "  d.code AS device,\n" +
//                "  s.code AS sensor,\n" +
//                "  (select min(data.date) from Data AS data where data.sensor.device.platform = p) as min_time,\n" +
//                "  (select min(data.date) from Data AS data where data.sensor.device.platform = p) as max_time,\n" +
//                "  (select min(data.value) from Data AS data where data.sensor = pos.latitude) as min_lat,\n" +
//                "  (select max(data.value) from Data AS data where data.sensor = pos.latitude) as max_lat,\n" +
//                "  (select min(data.value) from Data AS data where data.sensor = pos.longitude) as min_lon,\n" +
//                "  (select max(data.value) from Data AS data where data.sensor = pos.longitude) as max_lon\n" +
//                "from PositionInfo as pos\n" +
//                "  pos.sensor as s\n" +
//                "  pos.device as d\n" +
//                "  pos.platform as p\n" +
//                "where s.code is not null\n" +
//                "  and d.code is not null\n" +
//                "  and p.code is not null\n" +
//                "group by p.code, d.code, s.code").list();
//        }).stream().collect(Collectors.groupingBy(t
//                -> String.format("%s:%s", t[0], t[1]).toLowerCase(), mapping(t -> {
//                                              MinMax<DateTime> time
//                                                      = new MinMax<>((DateTime) t[2],
//                                                                     (DateTime) t[3]);
//                                              Envelope envelope
//                                                      = new Envelope((double) t[6],
//                                                                     (double) t[7],
//                                                                     (double) t[4],
//                                                                     (double) t[5]);
//                                              return new NRTEnvelope(time, time, envelope);
//                                          }, Collector.of(NRTEnvelope::new, NRTEnvelope::extend, NRTEnvelope::extend))));

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
                            .add(Projections.property("platform.code"))
                            .add(Projections.property("device.code"))
                            .add(Projections.property("sensor.code")))
                    .add(Restrictions.isNotNull("platform.code"))
                    .add(Restrictions.isNotNull("device.code"))
                    .add(Restrictions.isNotNull("sensor.code"));

            List<Object[]> result = (List<Object[]>) c.list();
            return result.stream()
                    .collect(groupingBy(t -> String.format("%s:%s", t[0], t[1]).toLowerCase(),
                                        mapping(t -> (String) t[2], toList())))
                    .entrySet()
                    .stream()
                    .map(e -> new NRTProcedure(e.getKey(), null, null, null, null, asOutputs(e.getValue())))
                    .collect(toList());
        });
    }

    private Set<NRTProcedureOutput> asOutputs(Collection<String> outputs) {
        return outputs.stream().map(o -> new NRTProcedureOutput(o, null)).collect(toSet());
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
