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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import org.joda.time.DateTime;
import org.locationtech.jts.geom.Envelope;

import org.n52.shetland.util.MinMax;



/**
 * A spatio-temporal envelope with an optional identifier.
 *
 * @author Christian Autermann
 */
public class SpaceTimeEnvelope {

    private final String identifier;
    private final MinMax<DateTime> time;
    private final Envelope envelope;

    /**
     * Create a new {@code SpaceTimeEnvelope}.
     *
     * @param identifier the identifier
     * @param time       the temporal component
     * @param envelope   the spatial component
     */
    public SpaceTimeEnvelope(@Nullable String identifier,
                             MinMax<DateTime> time,
                             Envelope envelope) {
        this.time = Objects.requireNonNull(time);
        this.envelope = Objects.requireNonNull(envelope);
        this.identifier = identifier;
    }

    /**
     * Create a empty {@code SpaceTimeEnvelope} with an identifier.
     *
     * @param identifier the identifier
     */
    public SpaceTimeEnvelope(@Nullable String identifier) {
        this(identifier, new MinMax<>(), new Envelope());
    }

    /**
     * Creates a new {@code SpaceTimeEnvelope}.
     *
     * @param time  the temporal component
     * @param space the spatial component
     */
    public SpaceTimeEnvelope(MinMax<DateTime> time, Envelope space) {
        this(null, time, space);
    }

    /**
     * Creates an empty {@code SpaceTimeEnvelope}.
     */
    public SpaceTimeEnvelope() {
        this(null);
    }

    /**
     * Get the temporal component of this envelope.
     *
     * @return the temporal envelope
     */
    public MinMax<DateTime> getTime() {
        return this.time;
    }

    /**
     * Get the spatial component of this envelope.
     *
     * @return the spatial envelope
     */
    public Envelope getSpace() {
        return envelope;
    }

    /**
     * Extend this envelope to include the {@code envelope}.
     *
     * @param envelope the envelope
     *
     * @return {@code this}
     */
    public SpaceTimeEnvelope extend(SpaceTimeEnvelope envelope) {
        this.time.extend(envelope.getTime(), DateTime::compareTo);
        if (envelope.getSpace() != null) {
            this.envelope.expandToInclude(envelope.getSpace());
        }
        return this;
    }

    /**
     * Checks if this envelope is empty.
     *
     * @return if it is empty
     */
    public boolean isEmpty() {
        return this.envelope.isNull();
    }

    /**
     * Get the identifier of this envelope.
     *
     * @return the identifier (may be {@code null})
     */
    @CheckForNull
    public String getIdentifier() {
        return identifier;
    }
}
