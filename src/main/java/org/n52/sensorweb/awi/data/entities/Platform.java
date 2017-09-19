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

/**
 * Entity for the {@code platform} table.
 *
 * @author Christian Autermann
 */
public class Platform implements Serializable {
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String CODE = "code";
    public static final String GEOMETRY = "geometry";
    public static final String EXPEDITIONS = "expeditions";
    public static final String PUBLISHED = "published";

    private static final long serialVersionUID = 763090253133294552L;
    private int id;
    private String name;
    private String code;
    private Geometry geometry;
    private Set<Expedition> expeditions = new HashSet<>(0);
    private boolean published;

    /**
     * Get the id of this platform.
     *
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Set the id of this platform.
     *
     * @param id the id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the name of this platform.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of this platform.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the code of this platform.
     *
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Set the code of this platform.
     *
     * @param code the code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Checks if this platform is mobile.
     *
     * @return if it is mobile
     */
    public boolean isMobile() {
        return this.geometry == null;
    }

    /**
     * Checks if this platform is stationary.
     *
     * @return if it is stationary
     */
    public boolean isStationary() {
        return this.geometry != null;
    }

    /**
     * Get the geomtry of this platform.
     *
     * @return the geometry
     */
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Set the geometry of this platform.
     *
     * @param geometry the geometry
     */
    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Get the expeditions of this platform.
     *
     * @return the expeditions
     */
    public Set<Expedition> getExpeditions() {
        return expeditions;
    }

    /**
     * Set the expeditions of this platform.
     *
     * @param expeditions the expeditions
     */
    public void setExpeditions(Set<Expedition> expeditions) {
        this.expeditions = expeditions;
    }

    /**
     * Checks if this platform is public.
     *
     * @return if it is public
     */
    public boolean isPublished() {
        return published;
    }

    /**
     * Sets if this platfomr is public.
     *
     * @param published if it is public
     */
    public void setPublished(boolean published) {
        this.published = published;
    }

}
