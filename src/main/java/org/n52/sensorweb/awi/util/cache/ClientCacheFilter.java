package org.n52.sensorweb.awi.util.cache;

import java.io.IOException;
import java.util.function.Consumer;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.n52.janmayen.function.Consumers;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
@Provider
public class ClientCacheFilter implements ClientRequestFilter, ClientResponseFilter {

    private final Cache cache;

    public ClientCacheFilter(Cache cache) {
        this.cache = cache;
    }

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        cache.getEntry(request).ifPresent(entry -> {
            if (!entry.isExpired()) {
                request.abortWith(entry.getResponse());
            } else {
                entry.getETag().ifPresent(addHeader(request, HttpHeaders.IF_NONE_MATCH));
                entry.getLastModified().ifPresent(addHeader(request, HttpHeaders.IF_MODIFIED_SINCE));
            }
        });
    }

    @Override
    public void filter(ClientRequestContext request, ClientResponseContext response) throws IOException {
        if (response.getStatusInfo().equals(Status.NOT_MODIFIED)) {
            CacheEntry entry = cache.getEntry(request).get();
            entry.update(response);
            entry.set(response);
        } else {
            cache.cache(request, response);
        }
    }

    private static Consumer<String> addHeader(ClientRequestContext request, String name) {
        return Consumers.curryFirst(request.getHeaders()::putSingle, name);
    }
}
