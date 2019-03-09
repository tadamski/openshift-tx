package org.jboss.as.quickstarts.xa.client;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("ejb")
public class EJBTestCallerRestEndpoints {

    @EJB
    private BeanTestServerCallerOnePhase serverCallerOnePhase;

    @EJB
    private BeanTestServerCallerTwoPhase serverCallerTwoPhase;

    @GET
    @Path("stateless-pass")
    @Produces("text/plain")
    public String testToPass() {
        BeanTestToPass bean = LookupHelper.lookupModuleEJB(BeanTestToPass.class, null);
        return bean.call();
    }

    @GET
    @Path("stateless-jvm-halt-business")
    @Produces("text/plain")
    public String testToHaltJVMOnBusinessMethod() {
        return serverCallerOnePhase.call("StatelessBeanKillJVMBusiness");
    }

    @GET
    @Path("stateless-jvm-halt-on-commit-server")
    @Produces("text/plain")
    public String testToHaltJVMOnCommitServer() {
        return serverCallerTwoPhase.callTestActionNone("StatelessBeanKillOnCommit");
    }

    @GET
    @Path("stateless-jvm-halt-on-prepare-server")
    @Produces("text/plain")
    public String BeanTestToKillJVMOnPrepareServer() {
        return serverCallerTwoPhase.callTestActionNone("StatelessBeanKillOnPrepare");
    }
}
