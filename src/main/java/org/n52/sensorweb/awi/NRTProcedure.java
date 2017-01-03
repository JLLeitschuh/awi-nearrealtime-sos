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

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.base.Strings;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class NRTProcedure {

    private final String id;
    private final Optional<String> longName;
    private final Optional<String> shortName;
    private final Optional<String> description;
    private final Optional<NRTProcedure> parent;
    private Set<NRTProcedure> children = Collections.emptySet();
    private final Set<NRTProcedureOutput> outputs;

    public NRTProcedure(String id, String shortName, String longName, String description, NRTProcedure parent,
                        Set<NRTProcedureOutput> outputs) {
        this.id = Objects.requireNonNull(Strings.emptyToNull(id));
        this.longName = Optional.ofNullable(Strings.emptyToNull(longName));
        this.shortName = Optional.ofNullable(Strings.emptyToNull(shortName));
        this.description = Optional.ofNullable(Strings.emptyToNull(description));
        this.parent = Optional.ofNullable(parent);
        this.outputs = Optional.ofNullable(outputs).orElseGet(Collections::emptySet);
    }

    public Optional<NRTProcedure> getParent() {
        return this.parent;
    }

    public NRTProcedure getPlatform() {
        NRTProcedure elem = this;
        while (elem.getParent().isPresent()) {
            elem = elem.getParent().get();
        }
        return elem;
    }

    public Set<NRTProcedure> getChildren() {
        return Collections.unmodifiableSet(this.children);
    }

    public void setChildren(Set<NRTProcedure> children) {
        this.children = Optional.ofNullable(children).orElseGet(Collections::emptySet);
    }

    public Set<NRTProcedureOutput> getOutputs() {
        return Collections.unmodifiableSet(this.outputs);
    }

    public String getId() {
        return id;
    }

    public Optional<String> getLongName() {
        return longName;
    }

    public Optional<String> getShortName() {
        return shortName;
    }

    public Optional<String> getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "Procedure{" + "id=" + id + ", parent=" + parent.map(NRTProcedure::getId) + ", children=" + children +
               ", outputs=" + outputs + '}';
    }

}
