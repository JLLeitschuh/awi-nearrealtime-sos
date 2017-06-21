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
package org.n52.sensorweb.awi.util;

import java.util.Objects;

import org.joda.time.DateTime;

import org.n52.shetland.util.MinMax;

import com.vividsolutions.jts.geom.Envelope;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class SpaceTimeEnvelope {

    private final String identifier;
    private final MinMax<DateTime> time;
    private final Envelope envelope;

    public SpaceTimeEnvelope(String identifier,
                             MinMax<DateTime> time,
                             Envelope envelope) {
        this.time = Objects.requireNonNull(time);
        this.envelope = Objects.requireNonNull(envelope);
        this.identifier = identifier;
    }

    public SpaceTimeEnvelope(String identifier) {
        this(identifier, new MinMax<>(), new Envelope());
    }

    public SpaceTimeEnvelope() {
        this(null);
    }

    public MinMax<DateTime> getTime() {
        return this.time;
    }

    public Envelope getSpace() {
        return envelope;
    }

    public SpaceTimeEnvelope extend(SpaceTimeEnvelope envelope) {
        this.time.extend(envelope.getTime(), DateTime::compareTo);
        if (envelope.getSpace() != null) {
            this.envelope.expandToInclude(envelope.getSpace());
        }
        return this;
    }

    public boolean isEmpty() {
        return this.envelope.isNull();
    }

    public String getIdentifier() {
        return identifier;
    }
}
