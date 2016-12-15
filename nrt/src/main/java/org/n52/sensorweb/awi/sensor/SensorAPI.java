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
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface SensorAPI {

    @Path("sensorOutputs")
    SensorOutputAPI sensorOutputs();

    @Path("device")
    DeviceAPI devices();

    @Path("platforms")
    PlatformAPI platforms();

    interface SensorOutputAPI {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getDeviceSensorOutputs/{device}")
        List<JsonSensorOutput> byDevice(@PathParam("device") int device);
    }

    interface DeviceAPI {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getChildrenOfDevice/{device}")
        List<JsonDevice> childrenOf(@PathParam("device") int device);

        @GET
        @Produces(MediaType.APPLICATION_XML)
        @Path("getDeviceAsSensorML/{device}")
        String getSensorML(@PathParam("device") int id);

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllActiveDevicesFast")
        List<JsonDevice> all();

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getDevice/{device}")
        JsonDevice byId(@PathParam("device") int device);

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllDeviceCategories")
        List<JsonType> types();

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getDeviceByUrn/{urn}")
        JsonDevice byURN(@PathParam("urn") String urn);
    }

    interface PlatformAPI {
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllRootItems")
        List<JsonDevice> all();

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("getAllPlatformTypes")
        List<JsonType> types();
    }

}
