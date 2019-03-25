package org.jboss.as.quickstarts.xa.client;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.jboss.as.quickstarts.xa.resources.MockXAResource;
import org.jboss.as.quickstarts.xa.resources.MockXAResource.TestAction;

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
    public String BeanTestToHaltJVMOnPrepareServer() {
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
    public String testToHaltJVMOnPrepareClient() {
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

    @GET
    @Path("stateless-programatic-pass")
    @Produces("text/plain")
    public String statelessProgrammaticToPass() {
        // calling remote StatelessToPassBean, look-up with programmatic way (no outbound connection here)
        StatelessServerProgramaticCallerTwoPhase bean = LookupHelper.lookupModuleEJB(StatelessServerProgramaticCallerTwoPhase.class);
        return bean.call("StatelessToPassBean", TestAction.NONE);
    }

    @GET
    @Path("stateless-programatic-jvm-halt-on-prepare-client")
    @Produces("text/plain")
    public String testProgrammaticToKillJVMOnPrepareClient() {
        // calling remote StatelessToPassBean, look-up with programmatic way (no outbound connection here)
        // killing JVM after prepare is called on the remote connection before the client MockXAResource is called
        StatelessServerProgramaticCallerTwoPhase bean = LookupHelper.lookupModuleEJB(StatelessServerProgramaticCallerTwoPhase.class);
        return bean.call("StatelessToPassBean", MockXAResource.TestAction.PREPARE_JVM_HALT);
    }
}
