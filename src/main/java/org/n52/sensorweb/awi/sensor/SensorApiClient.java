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
package org.n52.sensorweb.awi.sensor;

import java.util.List;
import java.util.Optional;

import org.n52.sensorweb.awi.sensor.json.JsonDevice;
import org.n52.sensorweb.awi.sensor.json.JsonSensorOutput;
import org.n52.sensorweb.awi.sensor.json.JsonType;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface SensorApiClient {
    List<JsonDevice> getChildren(JsonDevice device);
    List<JsonDevice> getChildren(int id);
    Optional<JsonDevice> getDevice(int id);
    Optional<JsonDevice> getDevice(String urn);
    List<JsonType> getDeviceCategories();
    List<JsonDevice> getDevices();
    Optional<JsonDevice> getPlatform(int id);
    List<JsonType> getPlatformTypes();
    List<JsonDevice> getPlatforms();
    Optional<JsonDevice> getRoot(int id);
    JsonDevice getRoot(JsonDevice device);
    Optional<String> getSensorML(JsonDevice device);
    Optional<String> getSensorML(int id);
    List<JsonSensorOutput> getSensorOutputs(JsonDevice device);
    List<JsonSensorOutput> getSensorOutputs(int device);
    void close();

}
