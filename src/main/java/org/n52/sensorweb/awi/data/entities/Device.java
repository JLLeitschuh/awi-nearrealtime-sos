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

/**
 * Entity for the {@code device} table.
 *
 * @author Christian Autermann
 */
public class Device implements Serializable {
    public static final String ID = "id";
    public static final String PLATFORM = "platform";
    public static final String NAME = "name";
    public static final String CODE = "code";

    private static final long serialVersionUID = -5686273001528029648L;
    private int id;
    private Platform platform;
    private String name;
    private String code;

    /**
     * Get the id of this device.
     *
     * @return the id
     */
    public int getId() {
        return this.id;
    }

    /**
     * Set the id of this device.
     *
     * @param id the id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get the platform of this device
     *
     * @return the platform
     */
    public Platform getPlatform() {
        return this.platform;
    }

    /**
     * Set the platform of this device.
     *
     * @param platform
     */
    public void setPlatform(Platform platform) {
        this.platform = platform;
    }

    /**
     * Get the name of this device.
     *
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of this device.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the code of this device.
     *
     * @return the code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Set the code of this device.
     *
     * @param code the code
     */
    public void setCode(String code) {
        this.code = code;
    }

}
