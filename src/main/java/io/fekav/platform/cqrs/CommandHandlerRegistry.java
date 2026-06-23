package io.fekav.platform.cqrs;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class CommandHandlerRegistry {

    private final Instance<CommandHandler<?, ?>> handlers;

    @Inject
    public CommandHandlerRegistry(
        @Any Instance<CommandHandler<?, ?>> handlers
    ) {
        this.handlers = handlers;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Command<?>> commandType(String commandName) {
        return handlers
            .stream()
            .map(CommandHandler::commandType)
            .filter(commandType ->
                commandType.getName().equals(commandName) ||
                    commandType.getSimpleName().equals(commandName)
            )
            .map(commandType -> (Class<? extends Command<?>>) commandType)
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No handler found for command: " + commandName
                )
            );
    }

    @SuppressWarnings("unchecked")
    public <R, C extends Command<R>> CommandHandler<R, C> getHandler(
        C command
    ) {
        return (CommandHandler<R, C>) handlers
            .stream()
            .filter(h -> h.commandType().isAssignableFrom(command.getClass()))
            .findFirst()
            .orElseThrow(() ->
                new IllegalArgumentException(
                    "No handler found for command: " +
                        command.getClass().getName()
                )
            );
    }
}
