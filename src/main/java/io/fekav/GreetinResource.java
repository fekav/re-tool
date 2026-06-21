package io.fekav;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hi")
public class GreetinResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hi() {
        return "was geht";
    }

}
