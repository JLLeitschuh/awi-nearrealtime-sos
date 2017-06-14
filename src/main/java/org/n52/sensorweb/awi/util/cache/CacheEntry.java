package org.n52.sensorweb.awi.util.cache;

import java.util.Optional;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.Response;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public interface CacheEntry {

    void update(ClientResponseContext response);

    void set(ClientResponseContext response);

    Optional<String> getLastModified();

    boolean isExpired();

    Response getResponse();

    Optional<String> getETag();

}
