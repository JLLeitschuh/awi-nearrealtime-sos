package org.n52.sensorweb.awi.util.cache;

import java.util.Optional;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface Cache {

    Optional<CacheEntry> getEntry(ClientRequestContext request);

    void cache(ClientRequestContext request, ClientResponseContext response);

}
