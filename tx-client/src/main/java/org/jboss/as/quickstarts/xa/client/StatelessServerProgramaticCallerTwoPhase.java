package org.jboss.as.quickstarts.xa.client;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;

import org.jboss.as.quickstarts.xa.resources.MockXAResource;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

/**
 * <p>
 * Bean which enlists a {@link MockXAResource} and then does the EJB remote
 * call to the second server. The EJB is lookuped with remoting properties
 * defined in the code. Outbound connection is not used.
 */
@Stateless
public class StatelessServerProgramaticCallerTwoPhase {
    private static final Logger log = Logger.getLogger(StatelessServerProgramaticCallerTwoPhase.class);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager manager;

    public String call(String beanName, PlaceToEnlist whereToEnlist, MockXAResource.TestAction testAction) {
        try {
            StatelessRemote bean = LookupHelper.lookupRemoteEJBDirect(beanName, StatelessRemote.class, false, null);

            if(PlaceToEnlist.BEFORE_REMOTE_EJB == whereToEnlist) {
            	manager.getTransaction().enlistResource(new MockXAResource(testAction));
            }

            int status = bean.call();
            log.infof("Transaction status from programatic 'call' is %s", status);

            if(PlaceToEnlist.AFTER_REMOTE_EJB == whereToEnlist) {
            	manager.getTransaction().enlistResource(new MockXAResource(testAction));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error on calling bean " + beanName + " programatically looked-up", e);
        }

        return "SUCCESS";
    }

    public String call(String beanName) {
        return this.call(beanName, PlaceToEnlist.BEFORE_REMOTE_EJB, MockXAResource.TestAction.NONE);
    }
}
