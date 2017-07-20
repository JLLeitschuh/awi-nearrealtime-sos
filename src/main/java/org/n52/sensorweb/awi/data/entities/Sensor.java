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
// Generated Oct 18, 2016 9:10:16 AM by Hibernate Tools 4.3.1

import java.io.Serializable;

/**
 * Entity for the {@code sensor} table.
 *
 * @author Christian Autermann
 */
public class Sensor implements Serializable {
    public static final String ID = "id";
    public static final String DEVICE = "device";
    public static final String NAME = "name";
    public static final String UNIT = "unit";
    public static final String CODE = "code";
    private static final long serialVersionUID = 2881635032211259816L;
    private int id;
    private Device device;
    private String name;
    private String unit;
    private String code;

    /**
     * Get the id of this sensor.
     *
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Sets the id of this sensor.
     *
     * @param id the id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the device of this sensor.
     *
     * @return the device
     */
    public Device getDevice() {
        return this.device;
    }

    /**
     * Sets the device of this sensor.
     *
     * @param device the device
     */
    public void setDevice(Device device) {
        this.device = device;
    }

    /**
     * Get the name of this sensor.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of this sensor.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the unit of this sensor.
     *
     * @return the unit
     */
    public String getUnit() {
        return this.unit;
    }

    /**
     * Sets the unit of this sensor.
     *
     * @param unit the unit
     */
    public void setUnit(String unit) {
        this.unit = unit;
    }

    /**
     * Get the code of this sensor.
     *
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Sets the code of this sensor.
     *
     * @param code the code
     */
    public void setCode(String code) {
        this.code = code;
    }

}
