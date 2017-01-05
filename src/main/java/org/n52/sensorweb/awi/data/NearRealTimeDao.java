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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joda.time.DateTime;

import org.n52.iceland.util.MinMax;
import org.n52.sensorweb.awi.NRTEnvelope;
import org.n52.sensorweb.awi.NRTProcedure;
import org.n52.sensorweb.awi.data.entities.Device;
import org.n52.sensorweb.awi.data.entities.Platform;
import org.n52.sensorweb.awi.data.entities.Sensor;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface NearRealTimeDao {
    Collection<NRTProcedure> getProcedures();

    Optional<Device> getDevice(int id);

    Optional<Device> getDevice(String code);

    List<Device> getDevices();

    Optional<Platform> getPlatform(int id);

    Optional<Platform> getPlatform(String code);

    List<Platform> getPlatforms();

    Optional<Sensor> getSensor(int id);

    Optional<Sensor> getSensor(String code);

    List<Sensor> getSensors();

    Optional<Sensor> getSeries(String urn);

    Map<String, MinMax<DateTime>> getMinMax();

    Map<String, NRTEnvelope> getEnvelopes();
}
