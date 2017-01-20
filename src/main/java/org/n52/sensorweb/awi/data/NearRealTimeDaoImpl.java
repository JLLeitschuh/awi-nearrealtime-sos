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

import org.n52.sensorweb.awi.NRTEnvelope;
import org.n52.sensorweb.awi.NRTProcedure;
import org.n52.sensorweb.awi.NRTProcedureOutput;
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
    private static final String SENSOR = "sensor";
    private static final String DEVICE = "device";
    private static final String PLATFORM = "platform";
    private static final String DATA = "data";

    public NearRealTimeDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Device> getDevices() {
        return query(s -> s.createCriteria(Device.class).add(Restrictions.isNotNull(Device.CODE)).list());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Sensor> getSensors() {
        return query(s -> s.createCriteria(Sensor.class).add(Restrictions.isNotNull(Sensor.CODE)).list());
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Platform> getPlatforms() {
        return query(s -> s.createCriteria(Platform.class).add(Restrictions.isNotNull(Platform.CODE)).list());
    }

    @Override
    public Optional<Device> getDevice(int id) {
        return queryOptional(s -> s.createCriteria(Device.class).add(Restrictions.eq(Device.ID, id)).uniqueResult());
    }

    @Override
    public Optional<Sensor> getSensor(int id) {
        return queryOptional(s -> s.createCriteria(Sensor.class).add(Restrictions.eq(Sensor.ID, id)).uniqueResult());
    }

    @Override
    public Optional<Platform> getPlatform(int id) {
        return queryOptional(s -> s.createCriteria(Platform.class).add(Restrictions.eq(Platform.ID, id)).uniqueResult());
    }

    @Override
    public Optional<Device> getDevice(String code) {
        return queryOptional(s -> s.createCriteria(Device.class)
                .add(Property.forName(Device.CODE).eq(code).ignoreCase())
                .uniqueResult());
    }

    @Override
    public Optional<Sensor> getSensor(String code) {
        return queryOptional(s -> s.createCriteria(Sensor.class)
                .add(Property.forName(Sensor.CODE).eq(code).ignoreCase())
                .uniqueResult());
    }

    @Override
    public Optional<Platform> getPlatform(String code) {
        return queryOptional(s -> s.createCriteria(Platform.class)
                .add(Property.forName(Platform.CODE).eq(code).ignoreCase())
                .uniqueResult());
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, NRTEnvelope> getEnvelopes() {
        return query(s -> {
            Criteria c = s.createCriteria(DataView.class, DATA);
            c.createAlias(PropertyPath.of(DATA, DataView.SENSOR), SENSOR);
            c.createAlias(PropertyPath.of(SENSOR, Sensor.DEVICE), DEVICE);
            c.createAlias(PropertyPath.of(DEVICE, Device.PLATFORM), PLATFORM);
            return (List<Object[]>) c.setProjection(Projections.projectionList()
                    .add(Projections.groupProperty(PropertyPath.of(PLATFORM, Platform.TYPE)))
                    .add(Projections.groupProperty(PropertyPath.of(PLATFORM, Platform.CODE)))
                    .add(Projections.groupProperty(PropertyPath.of(DEVICE, Device.CODE)))
                    .add(Projections.min(PropertyPath.of(DATA, DataView.TIME)))
                    .add(Projections.max(PropertyPath.of(DATA, DataView.TIME)))
                    .add(Projections.min(PropertyPath.of(DATA, DataView.LONGITUDE)))
                    .add(Projections.max(PropertyPath.of(DATA, DataView.LONGITUDE)))
                    .add(Projections.min(PropertyPath.of(DATA, DataView.LATITUDE)))
                    .add(Projections.max(PropertyPath.of(DATA, DataView.LATITUDE))))
                    .add(Restrictions.isNotNull(PropertyPath.of(PLATFORM, Platform.CODE)))
                    .add(Restrictions.isNotNull(PropertyPath.of(DEVICE, Device.CODE)))
                    .add(Restrictions.isNotNull(PropertyPath.of(DATA, DataView.LATITUDE)))
                    .add(Restrictions.isNotNull(PropertyPath.of(DATA, DataView.LONGITUDE)))
                    .add(Restrictions.isNotNull(PropertyPath.of(DATA, DataView.TIME)))
                    .list();
        }).stream().collect(toMap(
                t -> String.format("%s:%s:%s", t[0], t[1], t[2]).toLowerCase(),
                t -> createEnvelope(new MinMax<>((DateTime) t[3], (DateTime) t[4]),
                                    new MinMax<>((double) t[5], (double) t[6]),
                                    new MinMax<>((double) t[7], (double) t[8]))));
    }

    @Override
    public Optional<Sensor> getSeries(String urn) {
        String[] split = urn.split(":");
        switch (split.length) {
            case 3:
                return queryOptional(s -> getSeries(s, split[0], split[1], split[2]));
            case 4:
                return queryOptional(s -> getSeries(s, split[1], split[2], split[3]));
            default:
                return Optional.empty();
        }
    }

    private Sensor getSeries(Session s, String platformCode, String deviceCode, String sensorCode)
            throws HibernateException {
        return (Sensor) s
                .createCriteria(Sensor.class)
                .add(Property.forName(Sensor.CODE).eq(sensorCode).ignoreCase())
                .createCriteria(Sensor.DEVICE, JoinType.INNER_JOIN)
                .add(Property.forName(Device.CODE).eq(deviceCode).ignoreCase())
                .createCriteria(Device.PLATFORM, JoinType.INNER_JOIN)
                .add(Property.forName(Platform.CODE).eq(platformCode).ignoreCase())
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<NRTProcedure> getProcedures() {
        return query((Session session) -> {
            Criteria c = session.createCriteria(Sensor.class, SENSOR)
                    .createAlias(PropertyPath.of(SENSOR, Sensor.DEVICE), DEVICE)
                    .createAlias(PropertyPath.of(Sensor.DEVICE, Device.PLATFORM), PLATFORM)
                    .setProjection(Projections.projectionList()
                            .add(Projections.property(PropertyPath.of(PLATFORM, Platform.TYPE)))
                            .add(Projections.property(PropertyPath.of(PLATFORM, Platform.CODE)))
                            .add(Projections.property(PropertyPath.of(DEVICE, Device.CODE)))
                            .add(Projections.property(PropertyPath.of(SENSOR, Sensor.CODE))))
                    .add(Restrictions.isNotNull(PropertyPath.of(PLATFORM, Platform.CODE)))
                    .add(Restrictions.isNotNull(PropertyPath.of(DEVICE, Device.CODE)))
                    .add(Restrictions.isNotNull(PropertyPath.of(SENSOR, Sensor.CODE)));
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

    private NRTEnvelope createEnvelope(MinMax<DateTime> time, MinMax<Double> longitude, MinMax<Double> latitude) {
        Envelope envelope = new Envelope(longitude.getMinimum(),
                                         longitude.getMaximum(),
                                         latitude.getMinimum(),
                                         latitude.getMaximum());
        return new NRTEnvelope(time, time, envelope);
    }

}
