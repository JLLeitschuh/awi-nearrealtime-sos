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
package org.n52.sensorweb.awi.sensor.json;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for a JSON device.
 *
 * @author Christian Autermann
 */
public class JsonDevice {

    @JsonProperty(value = "id")
    private int id;
    @JsonProperty(value = "shortName")
    private String shortName;
    @JsonProperty(value = "longName")
    private String longName;
    @JsonProperty(value = "description")
    private String description;
    @JsonProperty(value = "urn")
    private String urn;
    @JsonProperty(value = "type")
    private JsonType type;
    @JsonProperty(value = "parentId")
    private Integer parentId;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public JsonType getType() {
        return type;
    }

    public void setType(JsonType type) {
        this.type = type;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + this.id;
        hash = 59 * hash + Objects.hashCode(this.shortName);
        hash = 59 * hash + Objects.hashCode(this.longName);
        hash = 59 * hash + Objects.hashCode(this.description);
        hash = 59 * hash + Objects.hashCode(this.urn);
        hash = 59 * hash + Objects.hashCode(this.type);
        hash = 59 * hash + this.parentId;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final JsonDevice other = (JsonDevice) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Objects.equals(this.parentId, other.parentId)) {
            return false;
        }
        if (!Objects.equals(this.shortName, other.shortName)) {
            return false;
        }
        if (!Objects.equals(this.longName, other.longName)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        if (!Objects.equals(this.urn, other.urn)) {
            return false;
        }
        if (!Objects.equals(this.type, other.type)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Device{" + "id=" + id + ", shortName=" + shortName + ", longName=" + longName + ", description=" +
               description + ", urn=" + urn + ", type=" + type + ", parentId=" + parentId + '}';
    }

}
