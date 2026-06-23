package io.fekav.platform.api;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

public class RestExceptionMappers {

    public record ErrorResponse(String error) {}

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapBadRequest(BadRequestException exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = "Bad request";
        }

        return RestResponse.status(
            Response.Status.BAD_REQUEST,
            new ErrorResponse(message)
        );
    }
}