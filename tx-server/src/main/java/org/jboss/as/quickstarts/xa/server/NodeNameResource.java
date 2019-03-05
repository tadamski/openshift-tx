package org.jboss.as.quickstarts.xa.server;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public class NodeNameResource {

    @GET
    @Path("node")
    @Produces("text/plain")
    public String node() {
        return System.getProperty("jboss.node.name");
    }

}
