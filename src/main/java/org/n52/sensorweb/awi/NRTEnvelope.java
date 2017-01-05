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

import org.joda.time.DateTime;

import org.n52.iceland.util.MinMax;

import com.vividsolutions.jts.geom.Envelope;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NRTEnvelope {

    private final MinMax<DateTime> phenomenonTime;
    private final MinMax<DateTime> resultTime;
    private final Envelope envelope;

    public NRTEnvelope() {
        this(new MinMax<>(), new MinMax<>(), new Envelope());
    }

    public NRTEnvelope(MinMax<DateTime> phenomenonTime,
                       MinMax<DateTime> resultTime, Envelope envelope) {
        this.phenomenonTime = phenomenonTime;
        this.resultTime = resultTime;
        this.envelope = envelope;
    }

    public MinMax<DateTime> getPhenomenonTime() {
        return phenomenonTime;
    }

    public MinMax<DateTime> getResultTime() {
        return resultTime;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public NRTEnvelope extend(NRTEnvelope envelope) {
        this.phenomenonTime.extend(envelope.getPhenomenonTime(), DateTime::compareTo);
        this.resultTime.extend(envelope.getResultTime(), DateTime::compareTo);
        if (envelope.getEnvelope() != null) {
            this.envelope.expandToInclude(envelope.getEnvelope());
        }
        return this;
    }
}
