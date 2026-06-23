package io.fekav.platform.cqrs;

public interface QueryBus {
    <R, Q extends Query<R>> R execute(Q query);
}
