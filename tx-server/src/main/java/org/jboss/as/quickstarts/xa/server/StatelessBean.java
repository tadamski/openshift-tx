package org.jboss.as.quickstarts.xa.server;

import java.rmi.RemoteException;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.jboss.as.quickstarts.xa.server.resource.MockXAResource;
import org.jboss.logging.Logger;

@Stateless
@Remote (StatelessRemote.class)
public class StatelessBean implements StatelessRemote {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    @Resource(lookup = "java:/TransactionManager")
    private TransactionManager manager;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() throws RemoteException {
        try {
            log.debug("Calling 'transactionStatus'");
            return manager.getStatus();
        } catch (SystemException e) {
            throw new RemoteException("Can't get transaction status", e);
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int call() throws RemoteException {
        try {
            log.debug("Calling 'call'");
            manager.getTransaction().enlistResource(new MockXAResource());
            return manager.getStatus();
        } catch (RollbackException | SystemException e) {
            throw new RemoteException("Cannot process with transaction", e);
        }
    }
}
