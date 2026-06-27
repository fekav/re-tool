package io.fekav.platform.adapter;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.cqrs.CommandHandler;
import io.fekav.platform.cqrs.CommandHandlerRegistry;
import io.fekav.platform.observability.Observability;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class DefaultCommandBus implements CommandBus {

    private static final Logger log = Logger.getLogger(DefaultCommandBus.class);

    private final CommandHandlerRegistry commandHandlerRegistry;

    @ConfigProperty(name = "observability.log.command-payload", defaultValue = "true")
    boolean logCommandPayload;

    @Inject
    public DefaultCommandBus(CommandHandlerRegistry commandHandlerRegistry) {
        this.commandHandlerRegistry = commandHandlerRegistry;
    }

    @Override
    public <R, C extends Command<R>> R dispatch(C command) {
        long startNanos = System.nanoTime();
        String commandName = command.getClass().getSimpleName();
        CommandHandler<R, C> handler = null;

        try {
            handler = commandHandlerRegistry.getHandler(command);
            String handlerName = handler.getClass().getSimpleName();

            log.info(
                Observability.event("command.start") + " " +
                    Observability.kv("command", commandName) + " " +
                    Observability.kv("handler", handlerName)
            );

            if (logCommandPayload) {
                log.info(
                    Observability.block(
                        "command.payload",
                        Observability.kv("command", commandName),
                        Observability.section("command_payload", command)
                    )
                );
            }

            R result = handler.handle(command);

            log.info(
                Observability.event("command.end") + " " +
                    Observability.kv("command", commandName) + " " +
                    Observability.kv("handler", handlerName) + " " +
                    Observability.kv("status", "ok") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos))
            );

            return result;
        } catch (RuntimeException | Error e) {
            log.error(
                Observability.event("command.end") + " " +
                    Observability.kv("command", commandName) + " " +
                    Observability.kv("handler", handler == null ? "unresolved" : handler.getClass().getSimpleName()) + " " +
                    Observability.kv("status", "error") + " " +
                    Observability.kv("duration_ms", Observability.durationMs(startNanos)) + " " +
                    Observability.kv("error_type", e.getClass().getSimpleName()) + " " +
                    Observability.kv("error_message", e.getMessage()),
                e
            );
            throw e;
        }
    }
}
