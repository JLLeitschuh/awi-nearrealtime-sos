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

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO for a JSON type.
 *
 * @author Christian Autermann
 */
public class JsonType {
    @JsonProperty(value = "vocabularyID")
    private String vocabularyId;
    @JsonProperty(value = "vocableValue")
    private URI vocableValue;
    @JsonProperty(value = "generalName")
    private String name;
    @JsonProperty(value = "description")
    private String description;
    @JsonProperty(value = "id")
    private int id;
    @JsonProperty(value = "vocableGroup")
    private JsonVocableGroup vocableGroup;

    public JsonVocableGroup getVocableGroup() {
        return vocableGroup;
    }

    public void setVocableGroup(JsonVocableGroup vocableGroup) {
        this.vocableGroup = vocableGroup;
    }

    public URI getVocableValue() {
        return vocableValue;
    }

    public void setVocableValue(URI vocableValue) {
        this.vocableValue = vocableValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getVocabularyId() {
        return vocabularyId;
    }

    public void setVocabularyId(String vocabularyId) {
        this.vocabularyId = vocabularyId;
    }

}
