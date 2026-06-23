package io.fekav.platform.cqrs;

public interface CommandHandler<R, C extends Command<R>> {
    R handle(C command);    
    Class<C> commandType();
}
