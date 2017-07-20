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
 * Client for {@link SensorAPI}.
 *
 * @author Christian Autermann
 */
public interface SensorAPIClient {
    /**
     * Get the children of the device.
     *
     * @param device the device
     *
     * @return the children
     */
    List<JsonDevice> getChildren(JsonDevice device);

    /**
     * Get the children of the device with the id {@code id}.
     *
     * @param id the id
     *
     * @return the children
     */
    List<JsonDevice> getChildren(int id);

    /**
     * Get the device with the specified id.
     *
     * @param id the id
     *
     * @return the device
     */
    Optional<JsonDevice> getDevice(int id);

    /**
     * Get the device with the specified URN.
     *
     * @param urn the URN
     *
     * @return the device
     */
    Optional<JsonDevice> getDevice(String urn);

    /**
     * Get the device categories.
     *
     * @return the categories
     */
    List<JsonType> getDeviceCategories();

    /**
     * Get the list of all devices.
     *
     * @return the devices
     */
    List<JsonDevice> getDevices();

    /**
     * Get the platform with the specified id.
     *
     * @param id the id
     *
     * @return the platform
     */
    Optional<JsonDevice> getPlatform(int id);

    /**
     * Get the list of all platform types.
     *
     * @return the platform types
     */
    List<JsonType> getPlatformTypes();

    /**
     * Get the list of all platforms.
     *
     * @return the platforms
     */
    List<JsonDevice> getPlatforms();

    /**
     * Get the root device of the device with the specified id.
     *
     * @param id the id
     *
     * @return the root device
     */
    Optional<JsonDevice> getRoot(int id);

    /**
     * Get the root device of the device.
     *
     * @param device the device
     *
     * @return the root device
     */
    JsonDevice getRoot(JsonDevice device);

    /**
     * Get the SensorML description of the specified device.
     *
     * @param device the device
     *
     * @return the SensorML description
     */
    Optional<String> getSensorML(JsonDevice device);

    /**
     * Get the SensorML description of the device with the specified id.
     *
     * @param id the id
     *
     * @return the SensorML description
     */
    Optional<String> getSensorML(int id);

    /**
     * Get the SensorML description of the device with the specified URN.
     *
     * @param urn the URN
     *
     * @return the SensorML description
     */
    Optional<String> getSensorML(String urn);

    /**
     * Get the outputs of the device.
     *
     * @param device the device
     *
     * @return the list of outputs
     */
    List<JsonSensorOutput> getSensorOutputs(JsonDevice device);

    /**
     * Get the outputs of the device with the specified id.
     *
     * @param id the id
     *
     * @return the outputs
     */
    List<JsonSensorOutput> getSensorOutputs(int id);

    /**
     * Close this client.
     */
    void close();

}
