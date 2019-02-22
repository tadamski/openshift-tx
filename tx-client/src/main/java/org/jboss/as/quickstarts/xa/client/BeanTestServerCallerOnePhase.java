package org.jboss.as.quickstarts.xa.client;

import javax.ejb.Stateless;

import org.jboss.as.quickstarts.xa.client.resources.Utils;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

@Stateless
public class BeanTestServerCallerOnePhase {
    private static final Logger log = Logger.getLogger(BeanTestServerCallerOnePhase.class);

    public String call(String beanName) {
        try {
            StatelessRemote bean = Utils.lookupRemoteEJBOutbound(beanName, StatelessRemote.class, null);
            int status = bean.call();
            log.debugf("Transaction status from 'call' is %s", status);
        } catch (Exception e) {
            throw new RuntimeException("Error on calling bean " + beanName, e);
        }

        return "SUCCESS";
    }
}
