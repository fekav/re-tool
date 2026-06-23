package io.fekav.platform.cqrs;

public interface QueryHandler<R, Q extends Query<R>> {
    
    R handle(Q query);

    Class<Q> queryType();
}
