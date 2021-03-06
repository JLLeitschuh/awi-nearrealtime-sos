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
package org.n52.sensorweb.awi.util.web;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * {@code Provider} for {@code ObjectMapper}.
 *
 * @author Christian Autermann
 */
@Provider
public class JSONConfiguration implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    /**
     * Creates a new JSONConfiguration using the specfied date format
     *
     * @param dateFormat the date format string
     */
    public JSONConfiguration(String dateFormat) {
        this.mapper = new ObjectMapper();
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        this.mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        this.mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        this.mapper.setDateFormat(getDateFormat(dateFormat));
    }

    /**
     * Creates a new {@code JSONConfiguration}.
     */
    public JSONConfiguration() {
        this("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }

    /**
     * Get the date format.
     *
     * @param format the format string
     *
     * @return the date format
     */
    private SimpleDateFormat getDateFormat(String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat;
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return this.mapper;
    }

}
