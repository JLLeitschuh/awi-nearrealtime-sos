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
package org.n52.sensorweb.awi.sensor;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class LoggingFilter implements WriterInterceptor, ClientRequestFilter, ClientResponseFilter {

    private final static String REQUEST_PREFIX = "> ";
    private final static String RESPONSE_PREFIX = "< ";
    private final static String ENTITY_LOGGER_PROPERTY = LoggingFilter.class.getName() + ".entityLogger";
    private final static String LOGGING_ID_PROPERTY = LoggingFilter.class.getName() + ".id";
    private final static String NOTIFICATION_PREFIX = "* ";
    private final static Set<MediaType> READABLE_APP_MEDIA_TYPES = new HashSet<>(Arrays.asList(
            new MediaType("text", "*"),
            new MediaType("text", "xml"),
            new MediaType("application", "xml")));
    private final static Comparator<Entry<String, List<String>>> COMPARATOR
            =  Comparator.comparing(Entry::getKey, String::compareToIgnoreCase);

    private static final Logger LOG = LoggerFactory.getLogger(LoggingFilter.class);
    private final AtomicLong id = new AtomicLong(0);
    private final int maxEntitySize = 0;

    public LoggingFilter() {
    }

    @Override
    public void filter(ClientRequestContext context) throws IOException {
        long currentId = this.id.incrementAndGet();
        context.setProperty(LOGGING_ID_PROPERTY, currentId);
        StringBuilder builder = new StringBuilder();
        printRequestLine(builder, "Sending client request", currentId, context.getMethod(), context.getUri());
        printPrefixedHeaders(builder, currentId, REQUEST_PREFIX, context.getStringHeaders());
        if (context.hasEntity() && isReadable(context.getMediaType())) {
            OutputStream stream = new LoggingStream(builder, context.getEntityStream());
            context.setEntityStream(stream);
            context.setProperty(ENTITY_LOGGER_PROPERTY, stream);
        } else {
            log(builder);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        Object requestId = requestContext.getProperty(LOGGING_ID_PROPERTY);
        long currentId = requestId != null ? (Long) requestId : this.id.incrementAndGet();
        StringBuilder b = new StringBuilder();
        printResponseLine(b, "Client response received", currentId, responseContext.getStatus());
        printPrefixedHeaders(b, currentId, RESPONSE_PREFIX, responseContext.getHeaders());
        if (responseContext.hasEntity() && isReadable(responseContext.getMediaType())) {
            responseContext.setEntityStream(logInboundEntity(b, responseContext.getEntityStream(), getCharset(responseContext.getMediaType())));
        }
        log(b);
    }

    /**
     * Logs a {@link StringBuilder} parameter at required level.
     *
     * @param b message to log
     */
    private void log(StringBuilder b) {
        LOG.info(b.toString());
    }

    private StringBuilder prefixId(StringBuilder b, long id) {
        return b.append(Long.toString(id)).append(" ");
    }

    private void printRequestLine(StringBuilder b, String note, long id, String method, URI uri) {
        prefixId(b, id).append(NOTIFICATION_PREFIX).append(note).append(" on thread ")
                .append(Thread.currentThread().getName()).append("\n");
        prefixId(b, id).append(REQUEST_PREFIX).append(method).append(" ").append(uri.toASCIIString()).append("\n");
    }

    private void printResponseLine(StringBuilder b, String note, long id, int status) {
        prefixId(b, id).append(NOTIFICATION_PREFIX).append(note).append(" on thread ")
                .append(Thread.currentThread().getName()).append("\n");
        prefixId(b, id).append(RESPONSE_PREFIX).append(Integer.toString(status)).append("\n");
    }

    private void printPrefixedHeaders(StringBuilder b, long id, String prefix, MultivaluedMap<String, String> headers) {
        headers.entrySet().stream().sorted(COMPARATOR).forEach(entry -> {
            List<?> val = entry.getValue();
            String header = entry.getKey();
            prefixId(b, id).append(prefix).append(header).append(": ");
            Iterator<?> iter = val.iterator();
            if (iter.hasNext()) {
                b.append(iter.next());
                while (iter.hasNext()) {
                    b.append(',');
                    b.append(iter.next());
                }
            }
            b.append("\n");
        });
    }

    private InputStream logInboundEntity(StringBuilder b, InputStream stream, Charset charset) throws IOException {
        if (!stream.markSupported()) {
            stream = new BufferedInputStream(stream);
        }
        stream.mark(maxEntitySize + 1);
        byte[] entity = new byte[maxEntitySize + 1];
        int entitySize = stream.read(entity);
        b.append(new String(entity, 0, Math.min(entitySize, maxEntitySize), charset));
        if (entitySize > maxEntitySize) {
            b.append("...more...");
        }
        b.append('\n');
        stream.reset();
        return stream;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException,
                                                                                        WebApplicationException {
        LoggingStream stream = (LoggingStream) writerInterceptorContext.getProperty(ENTITY_LOGGER_PROPERTY);
        writerInterceptorContext.proceed();
        if (stream != null && isReadable(writerInterceptorContext.getMediaType())) {
            log(stream.getStringBuilder(getCharset(writerInterceptorContext.getMediaType())));
        }
    }

    private static Charset getCharset(MediaType mt) {
        return Optional.ofNullable(mt).map(MediaType::getParameters)
                .map(m -> m.get(MediaType.CHARSET_PARAMETER))
                .map(Charset::forName).orElse(StandardCharsets.UTF_8);
    }

    private static boolean isReadable(MediaType mediaType) {
        return mediaType != null && READABLE_APP_MEDIA_TYPES.stream()
                .anyMatch(readableMediaType -> readableMediaType.isCompatible(mediaType));
    }

    private class LoggingStream extends FilterOutputStream {
        private final StringBuilder b;
        private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        LoggingStream(StringBuilder b, OutputStream inner) {
            super(inner);
            this.b = b;
        }

        StringBuilder getStringBuilder(Charset charset) {
            // write entity to the builder
            byte[] entity = baos.toByteArray();
            b.append(new String(entity, 0, Math.min(entity.length, maxEntitySize), charset));
            if (entity.length > maxEntitySize) {
                b.append("...more...");
            }
            b.append('\n');
            return b;
        }

        @Override
        public void write(int i) throws IOException {
            if (baos.size() <= maxEntitySize) {
                baos.write(i);
            }
            out.write(i);
        }
    }

}
