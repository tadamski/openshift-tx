package org.jboss.as.quickstarts.xa.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class NodeNameResource {

    @GET
    @Path("node")
    @Produces("text/plain")
    public String node() {
        return System.getProperty("jboss.node.name");
    }

    @GET
    @Path("ready")
    @Produces("text/plain")
    public Response ready(){
        String node = node();
        return Integer.parseInt(node.substring(node.length()-1))%2 == 0 ? Response.ok(node, MediaType.TEXT_PLAIN_TYPE).build() : Response.status(Response.Status.NOT_FOUND).build();
    }

}
