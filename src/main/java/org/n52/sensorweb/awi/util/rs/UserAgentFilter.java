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
package org.n52.sensorweb.awi.util.rs;

import java.io.IOException;
import java.util.Objects;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
@Provider
public class UserAgentFilter implements ClientRequestFilter {

    private final String userAgent;

    public UserAgentFilter(String userAgent) {
        this.userAgent = Objects.requireNonNull(userAgent);
    }

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        ctx.getHeaders().add(HttpHeaders.USER_AGENT, userAgent);
    }

}
