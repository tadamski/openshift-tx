package org.jboss.as.quickstarts.xa.client;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.jboss.as.quickstarts.xa.resources.StatusUtils;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BeanTestToPass {
    private static final Logger log = Logger.getLogger(BeanTestToPass.class);
    private static final String BEAN_NAME = "StatelessToPassBean";

    @Resource
    private UserTransaction userTransaction;

    public String call() {
        StatelessRemote bean;
        try {
            bean = LookupHelper.lookupRemoteEJBOutbound(BEAN_NAME, StatelessRemote.class, null);

            log.infof("Calling remote bean '%s' to find out the status of transaction", bean);
            int status = bean.transactionStatus();
            log.infof("Transaction status from 'transactionStatus' is %s", status);
            if(Status.STATUS_NO_TRANSACTION != status) {
                return "ERROR: No transaction expected but transaction status was " + StatusUtils.status(status);
            }
        } catch (NamingException ne) {
            log.errorf(ne, "Cannot lookup EJB bean '%s'", BEAN_NAME);
            return "ERROR: Cannot lookup EJB bean " + BEAN_NAME;
        } catch (RemoteException re) {
            log.errorf(re, "Error on calling remote bean '%s'", BEAN_NAME);
            return "Error on calling remote bean " + BEAN_NAME;
        }

        // remote call transaction propagation
        try {
            userTransaction.begin();
            int status = bean.call();
            log.infof("Transaction status from 'call' is %s", status);
            if(status != Status.STATUS_ACTIVE) {
                return "ERROR: Active transaction expected but was " + StatusUtils.status(status);
            }
            userTransaction.commit();
        } catch (NotSupportedException  nse) {
            log.errorf(nse, "Cannot start transaction");
            return "ERROR to start transaction";
        } catch (RemoteException re) {
            log.errorf(re, "Error on calling remote bean '%s' with txn context passed along", BEAN_NAME);
            rollbackTxn();
            return "ERROR to call the remote";
        } catch (Exception e) {
            log.error("Cannot commit transaction", e);
            rollbackTxn();
            return "ERROR to commit";
        } 

        return "SUCCESS";
    }

    private void rollbackTxn() {
        try {
            if(userTransaction.getStatus() == Status.STATUS_ACTIVE)
                userTransaction.rollback();
        } catch (Exception re) {
            log.errorf(re, "Cannot rollback transaction '%s'", userTransaction);
        }
    }

}
