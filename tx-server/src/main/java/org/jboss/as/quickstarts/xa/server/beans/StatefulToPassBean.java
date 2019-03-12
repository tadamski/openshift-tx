package org.jboss.as.quickstarts.xa.server.beans;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.EJBException;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.quickstarts.xa.server.StatefulRemote;
import org.jboss.logging.Logger;

@Remote(StatefulRemote.class)
@Stateful
public class StatefulToPassBean implements SessionSynchronization, StatefulRemote {
    private static final Logger log = Logger.getLogger(StatefulToPassBean.class);

    private Boolean commitSucceeded;
    private boolean beforeCompletion = false;
    private Object transactionKey = null;
    private boolean rollbackOnlyBeforeCompletion = false;

    @Resource
    private SessionContext sessionContext;

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() {
        int status = transactionSynchronizationRegistry.getTransactionStatus();
        log.infof("StatefulBean:transactionStatus -> %s", status);
        return status;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void resetStatus() {
        log.info("StatefulBean:resetStatus");
        commitSucceeded = null;
        beforeCompletion = false;
        transactionKey = null;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void setRollbackOnlyBeforeCompletion(boolean rollbackOnlyBeforeCompletion) throws RemoteException {
        log.debug("StatefulBean:setRollbackOnlyBeforeCompletion");
        this.rollbackOnlyBeforeCompletion = rollbackOnlyBeforeCompletion;
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void sameTransaction(boolean first) throws RemoteException {
        log.info("StatefulBean:sameTransaction -> " + (first ? "first" : "next follow-up (second)"));
        if (first) {
            transactionKey = transactionSynchronizationRegistry.getTransactionKey();
        } else {
            if (transactionKey == null) {
                throw new NullPointerException("not expecting transactionKey being null on the second call where first!=true");
            }
            if (!transactionKey.equals(transactionSynchronizationRegistry.getTransactionKey())) {
                throw new RemoteException("Transaction on second call was not the same as on first call");
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public void rollbackOnly() throws RemoteException {
        log.debug("StatefulBean:rollbackOnly");
        this.sessionContext.setRollbackOnly();
    }

    public void ejbCreate() {

    }

    public void afterBegin() throws EJBException, RemoteException {

    }

    public void beforeCompletion() throws EJBException, RemoteException {
        log.debug("StatefulBean:beforeCompletion");
        beforeCompletion = true;

        if (rollbackOnlyBeforeCompletion) {
            this.sessionContext.setRollbackOnly();
        }
    }

    public void afterCompletion(final boolean committed) throws EJBException, RemoteException {
        log.debug("StatefulBean:afterCompletion");
        commitSucceeded = committed;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public Boolean getCommitSucceeded() {
        return commitSucceeded;
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public boolean isBeforeCompletion() {
        return beforeCompletion;
    }
}
