package io.fekav.platform.cqrs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class QueryHandlerRegistry {

    private final Instance<QueryHandler<?, ?>> handlers;

    @Inject
    public QueryHandlerRegistry(@Any Instance<QueryHandler<?, ?>> handlers) {
        this.handlers = handlers;
    }

    @SuppressWarnings("unchecked")
    public <R, Q extends Query<R>> QueryHandler<R, Q> getHandler(Q query) {
        return (QueryHandler<R, Q>) handlers.stream()
                .filter(h -> h.queryType().isAssignableFrom(query.getClass()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No handler found for query: " + query.getClass().getName()
                ));
    }
}
