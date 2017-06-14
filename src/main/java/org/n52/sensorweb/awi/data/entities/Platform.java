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
package org.n52.sensorweb.awi.data.entities;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.vividsolutions.jts.geom.Geometry;

public class Platform implements Serializable {
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String CODE = "code";
    public static final String GEOMETRY = "geometry";
    public static final String DEVICES = "devices";
    public static final String EXPEDITION = "expedition";

    private static final long serialVersionUID = 763090253133294552L;
    private int id;
    private String name;
    private String code;
    private Geometry geometry;
    private Set<Device> devices = new HashSet<>(0);
    private Set<Expedition> expeditions = new HashSet<>(0);

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return this.code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Set<Device> getDevices() {
        return this.devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }

    public boolean isMobile() {
        return this.geometry == null;
    }

    public boolean isStationary() {
        return this.geometry != null;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public Set<Expedition> getExpeditions() {
        return expeditions;
    }

    public void setExpeditions(Set<Expedition> expeditions) {
        this.expeditions = expeditions;
    }

}
