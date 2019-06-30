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
 * call to the second server.
 * <p>
 * Because the mock resource is enlisted first the transaction contains
 * the two resources and two-phase commit without optimization is used.
 */
@Stateless
public class StatelessServerCallerTwoPhase {
    private static final Logger log = Logger.getLogger(StatelessServerCallerTwoPhase.class);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager manager;

    public String call(String beanName, MockXAResource.TestAction testAction) {
        try {
            StatelessRemote bean = LookupHelper.lookupRemoteEJBOutbound(beanName, StatelessRemote.class, false, null);
            int status = bean.call();
            manager.getTransaction().enlistResource(new MockXAResource(testAction));
            log.infof("Transaction status from 'call' is %s", status);
        } catch (Exception e) {
            throw new RuntimeException("Error on calling bean " + beanName, e);
        }

        return "SUCCESS";
    }

    public String callTestActionNone(String beanName) {
        return this.call(beanName, MockXAResource.TestAction.NONE);
    }
}
