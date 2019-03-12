package org.jboss.as.quickstarts.xa.client;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.as.quickstarts.xa.resources.MockXAResource;

@Path("ejb")
public class EJBTestCallerRestEndpoints {

    @EJB
    private StatelessServerCallerOnePhase serverCallerOnePhase;

    @EJB
    private StatelessServerCallerTwoPhase serverCallerTwoPhase;

    @GET
    @Path("stateless-pass")
    @Produces("text/plain")
    public String statelessToPass() {
        // calling remote StatelessToPassBean
        StatelessBeanManagedToPass bean = LookupHelper.lookupModuleEJB(StatelessBeanManagedToPass.class);
        return bean.call();
    }

    @GET
    @Path("stateless-jvm-halt-business")
    @Produces("text/plain")
    public String testToHaltJVMOnBusinessMethod() {
        return serverCallerOnePhase.call("StatelessBeanKillJVMBusiness");
    }

    @GET
    @Path("stateless-jvm-halt-on-prepare-server")
    @Produces("text/plain")
    public String BeanTestToKillJVMOnPrepareServer() {
        return serverCallerTwoPhase.callTestActionNone("StatelessBeanKillOnPrepare");
    }

    @GET
    @Path("stateless-jvm-halt-on-commit-server")
    @Produces("text/plain")
    public String testToHaltJVMOnCommitServer() {
        return serverCallerTwoPhase.callTestActionNone("StatelessBeanKillOnCommit");
    }

    @GET
    @Path("stateless-jvm-halt-on-prepare-client")
    @Produces("text/plain")
    public String BeanTestToKillJVMOnPrepareClient() {
        return serverCallerTwoPhase.call("StatelessToPassBean", MockXAResource.TestAction.PREPARE_JVM_HALT);
    }

    @GET
    @Path("stateless-jvm-halt-on-commit-client")
    @Produces("text/plain")
    public String testToHaltJVMOnCommitClient() {
        return serverCallerTwoPhase.call("StatelessToPassBean", MockXAResource.TestAction.COMMIT_JVM_HALT);
    }


    @GET
    @Path("stateful-pass")
    @Produces("text/plain")
    public String statefulToPass() {
        // calling remote StatefulToPassBean
        StatefulBeanManagedToPass bean = LookupHelper.lookupModuleEJB(StatefulBeanManagedToPass.class);
        return bean.call();
    }
}
