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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.n52.sensorweb.awi.sensor.json.JsonDevice;
import org.n52.sensorweb.awi.sensor.json.JsonSensorOutput;
import org.n52.sensorweb.awi.sensor.json.JsonType;

/**
 * JAX-RS interface for the  <a href="https://sensor.awi.de/">AWI Sensor API</a>.
 *
 * @author Christian Autermann
 */
public interface SensorAPI {

    /**
     * Get the sensor output API.
     *
     * @return the API
     */
    @Path("sensorOutputs")
    SensorOutputAPI sensorOutputs();

    /**
     * Get the device API.
     *
     * @return the API
     */
    @Path("device")
    DeviceAPI devices();

    /**
     * Get the platform API.
     *
     * @return the API
     */
    @Path("platforms")
    PlatformAPI platforms();

    /**
     * JAX-RS interface for the sensor output resource.
     */
    interface SensorOutputAPI {
        /**
         * Get the outputs of the device
         *
         * @param device the device
         *
         * @return the outputs
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getDeviceSensorOutputs/{device}")
        List<JsonSensorOutput> byDevice(@PathParam("device") int device);
    }

    /**
     * JAX-RS interface for the device resource.
     */
    interface DeviceAPI {
        /**
         * Get the children of the device.
         *
         * @param device the device
         *
         * @return the children
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getChildrenOfDevice/{device}")
        List<JsonDevice> childrenOf(@PathParam("device") int device);

        /**
         * Get the SensorML description of the device.
         *
         * @param id the id
         *
         * @return the SensorML description
         */
        @GET
        @Produces(MediaType.APPLICATION_XML)
        @Path("getDeviceAsSensorML/{device}")
        String getSensorML(@PathParam("device") int id);

        /**
         * Get the list of all devices.
         *
         * @return the devices
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllActiveDevicesFast")
        List<JsonDevice> all();

        /**
         * Get the device.
         *
         * @param device the id
         *
         * @return the device
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getDevice/{device}")
        JsonDevice byId(@PathParam("device") int device);

        /**
         * Get all device categories.
         *
         * @return the device categories
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllDeviceCategories")
        List<JsonType> types();

        /**
         * Get the device.
         *
         * @param urn the URN
         *
         * @return the device
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getDeviceByUrn/{urn}")
        JsonDevice byURN(@PathParam("urn") String urn);
    }

    /**
     * JAX-RS interface for the platform resource.
     */
    interface PlatformAPI {
        /**
         * Get all platforms.
         *
         * @return the platforms
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllRootItems")
        List<JsonDevice> all();

        /**
         * Get all platform types.
         *
         * @return the platform types
         */
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllPlatformTypes")
        List<JsonType> types();
    }

}
