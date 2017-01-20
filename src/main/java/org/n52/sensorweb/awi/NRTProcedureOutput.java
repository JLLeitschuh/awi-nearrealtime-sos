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
package org.n52.sensorweb.awi;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NRTProcedureOutput {
    private final String code;
    private final String name;
    private final NRTUnit unit;

    public NRTProcedureOutput(String code, String name, NRTUnit unit) {
        this.code = code;
        this.name = name;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public NRTUnit getUnit() {
        return unit;
    }

    public String getCode() {
        return code;
    }

    @Override
    public String toString() {
        return "NRTProcedureOutput{" + "code=" + code + ", name=" + name + ", unit=" + unit + '}';
    }

}
