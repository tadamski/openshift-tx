package org.jboss.as.quickstarts.xa.server;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.transaction.TransactionSynchronizationRegistry;
import java.rmi.RemoteException;

@Stateless
@Remote (StatelessRemote.class)
public class StatelessBean implements StatelessRemote {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public int transactionStatus() throws RemoteException {
        int status = transactionSynchronizationRegistry.getTransactionStatus();

        System.out.printf("Server: transactionStatus returning status %d%n", status);

        return status;
    }
}
