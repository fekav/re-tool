package io.fekav.platform.adapter;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.cqrs.CommandHandlerRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultCommandBus implements CommandBus {

    private final CommandHandlerRegistry commandHandlerRegistry;

    @Inject
    public DefaultCommandBus(CommandHandlerRegistry commandHandlerRegistry) {
        this.commandHandlerRegistry = commandHandlerRegistry;
    }

    @Override
    public <R, C extends Command<R>> R dispatch(C command) {
        CommandHandler<R, C> handler = commandHandlerRegistry.getHandler(command);
        return handler.handle(command);
    }

    

}
