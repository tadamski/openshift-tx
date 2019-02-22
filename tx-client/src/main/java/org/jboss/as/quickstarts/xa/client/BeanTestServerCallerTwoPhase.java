package org.jboss.as.quickstarts.xa.client;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;

import org.jboss.as.quickstarts.xa.client.resources.MockXAResource;
import org.jboss.as.quickstarts.xa.client.resources.Utils;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

@Stateless
public class BeanTestServerCallerTwoPhase {
    private static final Logger log = Logger.getLogger(BeanTestServerCallerTwoPhase.class);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager manager;

    public String call(String beanName, MockXAResource.TestAction testAction) {
        try {
            StatelessRemote bean = Utils.lookupRemoteEJBOutbound(beanName, StatelessRemote.class, null);
            manager.getTransaction().enlistResource(new MockXAResource(testAction));
            int status = bean.call();
            log.infof("Transaction status from 'call' is %s", status);
        } catch (Exception e) {
            throw new RuntimeException("Error on calling bean " + beanName, e);
        }

        return "SUCCESS";
    }

    public String callNone(String beanName) {
        return this.call(beanName, MockXAResource.TestAction.NONE);
    }
}
