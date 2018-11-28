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

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.cache.BrowserCacheFeature;
import org.jboss.resteasy.client.jaxrs.cache.LightweightBrowserCache;

import org.n52.sensorweb.awi.sensor.json.JsonDevice;
import org.n52.sensorweb.awi.sensor.json.JsonSensorOutput;
import org.n52.sensorweb.awi.sensor.json.JsonType;
import org.n52.sensorweb.awi.util.web.JSONConfiguration;
import org.n52.sensorweb.awi.util.web.LoggingFilter;
import org.n52.sensorweb.awi.util.web.UserAgentFilter;

/**
 * JAX-RS implementation of {@link SensorAPIClient}.
 *
 * @author Christian Autermann
 */
public class SensorAPIClientImpl implements SensorAPIClient {
    private final SensorAPI api;
    private final ResteasyClient client;

    /**
     * Creates a new {@link SensorAPIClient}.
     *
     * @param uri the base URI of the API
     */
    public SensorAPIClientImpl(URI uri) {
        LightweightBrowserCache cache = new LightweightBrowserCache();
        cache.setMaxBytes(1024 * 1024 * 50);
        BrowserCacheFeature cacheFeature = new BrowserCacheFeature();
        cacheFeature.setCache(cache);

        this.client = new ResteasyClientBuilder()
                .disableTrustManager()
                .connectionPoolSize(10)
                .maxPooledPerRoute(10)
                .register(new LoggingFilter())
                .register(new JSONConfiguration())
                .register(cacheFeature)
                .register(new UserAgentFilter("52N-SOS"))
                .build();
        this.api = client.target(uri).proxy(SensorAPI.class);
    }

    @Override
    public Optional<JsonDevice> getPlatform(int id) {
        return getDevice(id);
    }

    @Override
    public List<JsonSensorOutput> getSensorOutputs(JsonDevice device) {
        return getSensorOutputs(device.getId());
    }

    @Override
    public List<JsonSensorOutput> getSensorOutputs(int device) {
        return this.api.sensorOutputs().byDevice(device);
    }

    @Override
    public List<JsonDevice> getChildren(JsonDevice device) {
        return getChildren(device.getId());
    }

    @Override
    public List<JsonDevice> getChildren(int id) {
        return this.api.devices().childrenOf(id);
    }

    @Override
    public Optional<String> getSensorML(JsonDevice device) {
        return getSensorML(device.getId());
    }

    @Override
    public Optional<String> getSensorML(int id) {
        return Optional.ofNullable(this.api.devices().getSensorML(id));
    }

    @Override
    public Optional<String> getSensorML(String urn) {
        return getDevice(urn).flatMap(this::getSensorML);
    }

    @Override
    public List<JsonDevice> getPlatforms() {
        return this.api.platforms().all();
    }

    @Override
    public List<JsonDevice> getDevices() {
        return this.api.devices().all();
    }

    @Override
    public Optional<JsonDevice> getDevice(int id) {
        return Optional.ofNullable(this.api.devices().byId(id));
    }

    @Override
    public Optional<JsonDevice> getDevice(String urn) {
        return Optional.ofNullable(this.api.devices().byURN(urn));
    }

    @Override
    public Optional<JsonDevice> getRoot(int id) {
        return getDevice(id).map(this::getRoot);
    }

    @Override
    public JsonDevice getRoot(JsonDevice device) {
        Integer parentId = device.getParentId();
        return parentId == null ? device : getRoot(getDevice(parentId).get());
    }

    @Override
    public List<JsonType> getDeviceCategories() {
        return this.api.devices().types();
    }

    @Override
    public List<JsonType> getPlatformTypes() {
        return this.api.platforms().types();
    }

    @Override
    public void close() {
        this.client.close();
    }
}
