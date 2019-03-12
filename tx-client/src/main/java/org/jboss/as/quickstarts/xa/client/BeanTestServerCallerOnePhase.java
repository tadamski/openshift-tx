package org.jboss.as.quickstarts.xa.client;

import javax.ejb.Stateless;

import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

/**
 * <p>
 * Bean which does the EJB remote call to the second server.
 * <p>
 * Because there is only one XAResource as part of the transaction
 * - which is the ejb remote call - the 1PC optimization is used
 * and the <code>commit</code> is invoked on the ejb remote XAResource
 * without <code>prepare</code>.
 */
@Stateless
public class BeanTestServerCallerOnePhase {
    private static final Logger log = Logger.getLogger(BeanTestServerCallerOnePhase.class);

    public String call(String beanName) {
        try {
            StatelessRemote bean = LookupHelper.lookupRemoteEJBOutbound(beanName, StatelessRemote.class, null);
            int status = bean.call();
            log.debugf("Transaction status from 'call' is %s", status);
        } catch (Exception e) {
            throw new RuntimeException("Error on calling bean " + beanName, e);
        }

        return "SUCCESS";
    }
}
