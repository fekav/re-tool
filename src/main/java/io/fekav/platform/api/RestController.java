package io.fekav.platform.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.cqrs.CommandHandlerRegistry;
import io.fekav.platform.cqrs.Query;
import io.fekav.platform.cqrs.QueryBus;
import io.fekav.platform.cqrs.QueryHandlerRegistry;
import io.fekav.platform.observability.Observability;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@Path("/app")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestController {

    private static final Logger log = Logger.getLogger(RestController.class);

    private final CommandBus commandBus;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final QueryBus queryBus;
    private final QueryHandlerRegistry queryHandlerRegistry;
    private final ObjectMapper objectMapper;

    @ConfigProperty(name = "observability.log.raw-request", defaultValue = "true")
    boolean logRawRequest;

    private record CommandRequest(String command, JsonNode payload) {}
    private record QueryRequest(String query, JsonNode payload) {}

    @Inject
    public RestController(
        CommandBus commandBus,
        CommandHandlerRegistry commandHandlerRegistry,
        QueryBus queryBus,
        QueryHandlerRegistry queryHandlerRegistry,
        ObjectMapper objectMapper
    ) {
        this.commandBus = commandBus;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.queryBus = queryBus;
        this.queryHandlerRegistry = queryHandlerRegistry;
        this.objectMapper = objectMapper;
    }

    @POST
    @Path("/c")
    public Response executeCommand(CommandRequest request) {
        // validate request
        if (
            request == null ||
            request.command() == null ||
            request.payload() == null
        ) {
            throw new BadRequestException("Request body error");
        }
        logCommandRequest(request);
        // validate command
        Class<? extends Command<?>> commandType = commandType(request.command());
        if (commandType == null) {
            throw new BadRequestException(
                "Unknown command: " + request.command()
            );
        }
        // create command object from request body
        JsonNode payload = request.payload();
        Command<?> cmd;
        try {
            cmd = payload.isTextual()
                ? objectMapper.readValue(payload.asText(), commandType)
                : objectMapper.treeToValue(payload, commandType);
        } catch (Exception e) {
            throw new BadRequestException("command payload error");
        }
        // dispatch command
        Object result;
        try {
            result = commandBus.dispatch(cmd);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @POST
    @Path("/q")
    public Response executeQuery(QueryRequest request) {
        if (
            request == null ||
            request.query() == null ||
            request.payload() == null
        ) {
            throw new BadRequestException("Request body error");
        }

        Class<? extends Query<?>> queryType = queryType(request.query());
        if (queryType == null) {
            throw new BadRequestException("Unknown query: " + request.query());
        }

        Query<?> query;
        try {
            JsonNode payload = request.payload();
            query = payload.isTextual()
                ? objectMapper.readValue(payload.asText(), queryType)
                : objectMapper.treeToValue(payload, queryType);
        } catch (Exception e) {
            throw new BadRequestException("query payload error");
        }

        Object result;
        try {
            result = queryBus.execute(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
        return Response.ok(result).build();
    }

    private Class<? extends Command<?>> commandType(String commandName) {
        try {
            return commandHandlerRegistry.commandType(commandName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private Class<? extends Query<?>> queryType(String queryName) {
        try {
            return queryHandlerRegistry.queryType(queryName);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    private void logCommandRequest(CommandRequest request) {
        if (logRawRequest) {
            log.info(
                Observability.block(
                    "api.command.received",
                    Observability.kv("command", request.command()),
                    Observability.section("command_payload", request.payload())
                )
            );
            String rawRequirementText = rawRequirementTextFrom(request.payload());
            if (rawRequirementText != null) {
                log.info(
                    Observability.block(
                        "api.raw_requirement_text",
                        Observability.kv("command", request.command()),
                        Observability.section("raw_requirement_text", rawRequirementText)
                    )
                );
            }
        } else {
            log.info(
                Observability.event("api.command.received") + " " +
                    Observability.kv("command", request.command())
            );
        }
    }

    private String rawRequirementTextFrom(JsonNode payload) {
        JsonNode readablePayload = payload;

        if (payload.isTextual()) {
            try {
                readablePayload = objectMapper.readTree(payload.asText());
            } catch (Exception e) {
                return null;
            }
        }

        JsonNode rawText = readablePayload.path("rawText");
        return rawText.isTextual() ? rawText.asText() : null;
    }
}
