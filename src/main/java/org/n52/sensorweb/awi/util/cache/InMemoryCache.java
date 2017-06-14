package org.n52.sensorweb.awi.util.cache;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.HttpHeaders;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class InMemoryCache implements Cache {
    @Override
    public Optional<CacheEntry> getEntry(ClientRequestContext request) {

        if (!isIndempotent(request.getMethod())) {
            return Optional.empty();
        }

        /* TODO implement org.n52.sensorweb.awi.util.cache.InMemoryCache.getEntry() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.util.cache.InMemoryCache.getEntry() not yet implemented");
    }

    @Override
    public void cache(ClientRequestContext request, ClientResponseContext response) {
        if (!isIndempotent(request.getMethod())) {
            return;
        }

        List<String> vary = response.getHeaders().get(HttpHeaders.VARY);
        List<String> cacheControl = response.getHeaders().get(HttpHeaders.CACHE_CONTROL);

        if (cacheControl.contains("no-cache") || cacheControl.contains("no-store")) {
            return;
        }


        /* TODO implement org.n52.sensorweb.awi.util.cache.InMemoryCache.cache() */
        throw new UnsupportedOperationException("org.n52.sensorweb.awi.util.cache.InMemoryCache.cache() not yet implemented");
    }

    private static boolean isIndempotent(String method) {
        if (method == null) {
            return false;
        }
        switch (method) {
            case HttpMethod.GET:
            case HttpMethod.HEAD:
            case HttpMethod.OPTIONS:
                return true;
            default:
                return false;
        }
    }

}
