package org.jboss.as.quickstarts.xa.server.beans;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.as.quickstarts.xa.resources.MockXAResource;
import org.jboss.as.quickstarts.xa.resources.StatusUtils;
import org.jboss.as.quickstarts.xa.server.StatelessRemote;
import org.jboss.logging.Logger;

@Stateless
@Remote (StatelessRemote.class)
public class StatelessToPassBean implements StatelessRemote {
    private static final Logger log = Logger.getLogger(StatelessToPassBean.class);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager manager;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() throws RemoteException {
        try {
            int status = manager.getStatus();
            log.infof("Calling 'transactionStatus', status is %s", StatusUtils.status(status));
            return status;
        } catch (SystemException e) {
            throw new RemoteException("Can't get transaction status", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int call() throws RemoteException {
        try {
            int status = manager.getStatus();
            log.infof("Calling 'call' with txn status %s", StatusUtils.status(status));
            manager.getTransaction().enlistResource(new MockXAResource());
            return status;
        } catch (RollbackException | SystemException e) {
            throw new RemoteException("Cannot process with transaction", e);
        }
    }
}
