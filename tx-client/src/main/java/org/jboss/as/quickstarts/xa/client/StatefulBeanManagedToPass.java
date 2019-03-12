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
import org.jboss.as.quickstarts.xa.server.StatefulRemote;
import org.jboss.logging.Logger;

/**
 * EJB using the {@link TransactionManagement} of <code>BEAN</code>
 * and makes first call without and then with the transaction context
 * while the Stateful bean is invoked.
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class StatefulBeanManagedToPass {
    private static final Logger log = Logger.getLogger(StatefulBeanManagedToPass.class);
    private static final String BEAN_NAME = "StatefulToPassBean";

    @Resource
    private UserTransaction userTransaction;

    public String call() {
        StatefulRemote bean;
        try {
            bean = LookupHelper.lookupRemoteStatefulEJBOutbound(BEAN_NAME, StatefulRemote.class);

            log.infof("Calling remote SFSB '%s' to find out the status of transaction", bean);
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

        try {
            bean.resetStatus();
        } catch (RemoteException reStatus) {
            log.errorf(reStatus, "Error on reseting status of remote bean '%s'", BEAN_NAME);
            return "Error on reseting status of remote bean " + BEAN_NAME;
        }

        // remote call transaction propagation
        try {
            // two calls in the same transaction, if the second call does not provide
            // the same transction then RemoteException is expected
            userTransaction.begin();
            bean.sameTransaction(false);
            bean.sameTransaction(true);
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
