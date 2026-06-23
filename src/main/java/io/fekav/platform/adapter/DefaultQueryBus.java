package io.fekav.platform.adapter;

import io.fekav.platform.cqrs.Query;
import io.fekav.platform.cqrs.QueryBus;
import io.fekav.platform.cqrs.QueryHandler;
import io.fekav.platform.cqrs.QueryHandlerRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultQueryBus implements QueryBus {

    private final QueryHandlerRegistry queryHandlerRegistry;

    @Inject
    public DefaultQueryBus(QueryHandlerRegistry queryHandlerRegistry) {
        this.queryHandlerRegistry = queryHandlerRegistry;
    }

    @Override
    public <R, Q extends Query<R>> R execute(Q query) {
        QueryHandler<R, Q> handler =
                queryHandlerRegistry.getHandler(query);

        return handler.handle(query);
    }
}
