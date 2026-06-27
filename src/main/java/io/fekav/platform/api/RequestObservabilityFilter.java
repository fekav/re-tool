package io.fekav.platform.api;

import java.util.Locale;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.fekav.platform.observability.Observability;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class RequestObservabilityFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(RequestObservabilityFilter.class);

    private static final String TRACEPARENT_HEADER = "traceparent";
    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String TRACE_ID_PROPERTY = RequestObservabilityFilter.class.getName() + ".traceId";
    private static final String START_NANOS_PROPERTY = RequestObservabilityFilter.class.getName() + ".startNanos";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String traceId = traceIdFrom(requestContext.getHeaderString(TRACEPARENT_HEADER));

        if (traceId == null) {
            traceId = requestContext.getHeaderString(REQUEST_ID_HEADER);
        }

        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        Observability.setTraceId(traceId);
        requestContext.setProperty(TRACE_ID_PROPERTY, traceId);
        requestContext.setProperty(START_NANOS_PROPERTY, System.nanoTime());

        log.info(
            Observability.event("http.request.start") + " " +
                Observability.kv("method", requestContext.getMethod()) + " " +
                Observability.kv("path", requestContext.getUriInfo().getPath())
        );
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        Object traceId = requestContext.getProperty(TRACE_ID_PROPERTY);

        if (traceId != null) {
            Observability.setTraceId(traceId.toString());
            responseContext.getHeaders().putSingle(REQUEST_ID_HEADER, traceId.toString());
        }

        Object startNanos = requestContext.getProperty(START_NANOS_PROPERTY);
        long durationMs = startNanos instanceof Long start ? Observability.durationMs(start) : -1;

        log.info(
            Observability.event("http.request.end") + " " +
                Observability.kv("method", requestContext.getMethod()) + " " +
                Observability.kv("path", requestContext.getUriInfo().getPath()) + " " +
                Observability.kv("status", responseContext.getStatus()) + " " +
                Observability.kv("duration_ms", durationMs)
        );

        Observability.clearTraceId();
    }

    private String traceIdFrom(String traceparent) {
        if (traceparent == null || traceparent.isBlank()) {
            return null;
        }

        String[] parts = traceparent.split("-");
        if (parts.length < 4) {
            return null;
        }

        String traceId = parts[1];
        if (
            traceId.length() == 32 &&
                traceId.matches("[0-9a-fA-F]{32}") &&
                !traceId.equals("00000000000000000000000000000000")
        ) {
            return traceId.toLowerCase(Locale.ROOT);
        }

        return null;
    }
}
