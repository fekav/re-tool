package io.fekav.platform.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fekav.platform.cqrs.Command;
import io.fekav.platform.cqrs.CommandBus;
import io.fekav.platform.cqrs.CommandHandlerRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/app")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestController {

    private final CommandBus commandBus;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final ObjectMapper objectMapper;

    private record CommandRequest(String command, JsonNode payload) {}

    @Inject
    public RestController(
        CommandBus commandBus,
        CommandHandlerRegistry commandHandlerRegistry,
        ObjectMapper objectMapper
    ) {
        this.commandBus = commandBus;
        this.commandHandlerRegistry = commandHandlerRegistry;
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
        // validate command
        Class<? extends Command<?>> commandType =
            commandHandlerRegistry.commandType(request.command());
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
        Object result = commandBus.dispatch(cmd);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }
}
